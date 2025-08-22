package com.example.bookstore.service;

import com.example.bookstore.entity.PointRule;
import com.example.bookstore.entity.PointsAccount;
import com.example.bookstore.entity.PointLots;
import com.example.bookstore.entity.PointsLedger;
import com.example.bookstore.entity.PointReservation;
import com.example.bookstore.repository.PointRuleRepository;
import com.example.bookstore.repository.PointsAccountRepository;
import com.example.bookstore.repository.PointsLedgerRepository;
import com.example.bookstore.repository.PointLotsRepository;
import com.example.bookstore.repository.PointReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {
    
    private final PointRuleRepository pointRuleRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final PointLotsRepository pointLotsRepository;
    private final PointReservationRepository pointReservationRepository;

    /**
     * 計算訂單可獲得的點數（amount 單位："yuan" 則以元；"cent" 則以分自動轉元）
     */
    public int calculateOrderPoints(BigDecimal orderAmount) {
        PointRule rule = getCurrentPointRule();
        if (rule == null) {
            return 0;
        }
        // 計算點數 (萬分比)
        int points = orderAmount.multiply(BigDecimal.valueOf(rule.getRewardRateBp()))
                               .divide(BigDecimal.valueOf(10000), 0, BigDecimal.ROUND_DOWN)
                               .intValue();
        if (rule.getMaxRewardPoints() != null && points > rule.getMaxRewardPoints()) {
            points = rule.getMaxRewardPoints();
        }
        return points;
    }
    
    /**
     * 計算點數可折抵的金額（redeemRate 為整數：元/點）
     */
    public BigDecimal calculatePointDeduction(int points, BigDecimal orderAmount) {
        PointRule rule = getCurrentPointRule();
        if (rule == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal deduction = BigDecimal.valueOf(points).multiply(BigDecimal.valueOf(rule.getRedeemRate()));
        if (rule.getMaxRedeemRatioBp() != null) {
            BigDecimal maxDeduction = orderAmount.multiply(BigDecimal.valueOf(rule.getMaxRedeemRatioBp()))
                                                .divide(BigDecimal.valueOf(10000), 2, BigDecimal.ROUND_DOWN);
            if (deduction.compareTo(maxDeduction) > 0) {
                deduction = maxDeduction;
            }
        }
        return deduction;
    }
    
    public PointRule getCurrentPointRule() {
        return pointRuleRepository.findFirstByOrderByIdAsc();
    }
    
    public PointRule updatePointRule(PointRule newRule) {
        PointRule currentRule = getCurrentPointRule();
        if (currentRule == null) {
            return pointRuleRepository.save(newRule);
        }
        currentRule.setRewardRateBp(newRule.getRewardRateBp());
        currentRule.setMaxRewardPoints(newRule.getMaxRewardPoints());
        currentRule.setRedeemRate(newRule.getRedeemRate());
        currentRule.setMaxRedeemRatioBp(newRule.getMaxRedeemRatioBp());
        currentRule.setExpiryPolicy(newRule.getExpiryPolicy());
        currentRule.setRollingDays(newRule.getRollingDays());
        currentRule.setFixedExpireDay(newRule.getFixedExpireDay());
        return pointRuleRepository.save(currentRule);
    }
    
    /**
     * 查詢可用點數（餘額 - 活躍中的預留）
     */
    public int getAvailablePoints(Long userId) {
        PointsAccount account = pointsAccountRepository.findByUserId(userId)
                .orElseGet(() -> pointsAccountRepository.save(newAccount(userId)));
        Integer reserved = pointReservationRepository.sumActiveReservedByUser(userId);
        return account.getBalance() - (reserved == null ? 0 : reserved);
    }

    /**
     * 預留點數（下單時鎖點），確保可用點數足夠並建立唯一 orderId 的預留。
     */
    public PointReservation reservePoints(Long userId, Long orderId, int points) {
        if (points <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "points 必須 > 0");
        // 若已有相同 orderId 的預留，回傳現有（若為 ACTIVE），否則阻止重複
        var existed = pointReservationRepository.findByOrderId(orderId).orElse(null);
        if (existed != null) {
            if (existed.getStatus() == PointReservation.Status.ACTIVE && Objects.equals(existed.getUserId(), userId)) {
                return existed; // idempotent
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此訂單已存在預留或已處理");
        }
        // 以帳戶悲觀鎖檢查可用點數
        PointsAccount account = pointsAccountRepository.lockByUserId(userId)
                .orElseGet(() -> pointsAccountRepository.save(newAccount(userId)));
        Integer reserved = pointReservationRepository.sumActiveReservedByUser(userId);
        int available = account.getBalance() - (reserved == null ? 0 : reserved);
        if (available < points) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "可用點數不足");
        }
        PointReservation r = new PointReservation();
        r.setUserId(userId);
        r.setOrderId(orderId);
        r.setReservedPts(points);
        r.setStatus(PointReservation.Status.ACTIVE);
        r.setCreatedAt(LocalDateTime.now());
        return pointReservationRepository.save(r);
    }

    /**
     * 確認預留（扣點），FIFO 消耗點數批次，寫入流水，更新餘額，預留設為 COMMITTED。
     */
    public void commitReservation(Long userId, Long orderId) {
        PointReservation r = pointReservationRepository.lockByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到預留"));
        if (!Objects.equals(r.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId 不一致");
        }
        if (r.getStatus() == PointReservation.Status.COMMITTED) {
            return; // idempotent
        }
        if (r.getStatus() != PointReservation.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "預留狀態不可確認");
        }
        // 鎖帳戶與可用批次
        PointsAccount account = pointsAccountRepository.lockByUserId(userId)
                .orElseGet(() -> pointsAccountRepository.save(newAccount(userId)));
        List<PointLots> lots = pointLotsRepository.lockAvailableLotsOrderByExpiry(userId);
        int need = r.getReservedPts();
        int remainFromLots = lots.stream().mapToInt(l -> l.getEarnedPoints() - l.getUsedPoints()).sum();
        if (remainFromLots < need) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "可用批次不足，可能因過期導致");
        }
        // FIFO 消耗
        int toConsume = need;
        for (PointLots lot : lots) {
            if (toConsume <= 0) break;
            int canUse = lot.getEarnedPoints() - lot.getUsedPoints();
            int use = Math.min(canUse, toConsume);
            lot.setUsedPoints(lot.getUsedPoints() + use);
            pointLotsRepository.save(lot);
            toConsume -= use;
        }
        // 扣帳戶餘額 & 記流水
        account.setBalance(account.getBalance() - need);
        pointsAccountRepository.save(account);

        PointsLedger ledger = new PointsLedger();
        ledger.setUserId(userId);
        ledger.setChangeAmount(-need);
        ledger.setReason(PointsLedger.Reason.REDEEM);
        ledger.setRelatedOrderId(orderId);
        ledger.setNote("Commit reservation");
        ledger.setBalanceAfter(account.getBalance());
        pointsLedgerRepository.save(ledger);

        // 更新預留狀態
        r.setStatus(PointReservation.Status.COMMITTED);
        r.setCommittedAt(LocalDateTime.now());
        pointReservationRepository.save(r);
    }

    /**
     * 取消預留（釋放），不變更帳戶餘額與批次，僅標記狀態。
     */
    public void cancelReservation(Long userId, Long orderId) {
        PointReservation r = pointReservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到預留"));
        if (!Objects.equals(r.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId 不一致");
        }
        if (r.getStatus() == PointReservation.Status.CANCELLED) return; // idempotent
        if (r.getStatus() == PointReservation.Status.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "預留已確認，無法取消");
        }
        r.setStatus(PointReservation.Status.CANCELLED);
        r.setCancelledAt(LocalDateTime.now());
        pointReservationRepository.save(r);
    }

    /**
     * 直接發點（例如付款完成的回饋）。
     */
    public void grantPoints(Long userId, Long relatedOrderId, int points, String note) {
        if (points <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "points 必須 > 0");
        PointsAccount account = pointsAccountRepository.lockByUserId(userId)
                .orElseGet(() -> pointsAccountRepository.save(newAccount(userId)));
        PointRule rule = getCurrentPointRule();
        LocalDateTime expiresAt = getPointExpiryDate(LocalDateTime.now(), rule);
        PointLots lot = new PointLots();
        lot.setUserId(userId);
        lot.setSource(PointLots.Source.ORDER);
        lot.setRelatedOrderId(relatedOrderId);
        lot.setEarnedPoints(points);
        lot.setUsedPoints(0);
        lot.setExpiresAt(expiresAt);
        pointLotsRepository.save(lot);

        account.setBalance(account.getBalance() + points);
        pointsAccountRepository.save(account);

        PointsLedger ledger = new PointsLedger();
        ledger.setUserId(userId);
        ledger.setChangeAmount(points);
        ledger.setReason(PointsLedger.Reason.PURCHASE_REWARD);
        ledger.setRelatedOrderId(relatedOrderId);
        ledger.setNote(note == null ? "Reward" : note);
        ledger.setBalanceAfter(account.getBalance());
        pointsLedgerRepository.save(ledger);
    }

    /**
     * 後台調整點數：delta 可為正（加點）或負（扣點）。
     * 新版支援覆寫到期天數（僅對加點有效）。
     */
    public void adjustPoints(Long userId, int delta, String note, Integer overrideExpiryDays) {
        if (delta == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "delta 不可為 0");
        PointsAccount account = pointsAccountRepository.lockByUserId(userId)
                .orElseGet(() -> pointsAccountRepository.save(newAccount(userId)));
        if (delta > 0) {
            // 加點：新增調整批次
            PointRule rule = getCurrentPointRule();
            LocalDateTime expiresAt;
            if (overrideExpiryDays != null && overrideExpiryDays > 0) {
                expiresAt = LocalDateTime.now().plusDays(overrideExpiryDays);
            } else {
                expiresAt = getPointExpiryDate(LocalDateTime.now(), rule);
            }
            PointLots lot = new PointLots();
            lot.setUserId(userId);
            lot.setSource(PointLots.Source.ADJUSTMENT);
            lot.setRelatedOrderId(null);
            lot.setEarnedPoints(delta);
            lot.setUsedPoints(0);
            lot.setExpiresAt(expiresAt);
            pointLotsRepository.save(lot);

            account.setBalance(account.getBalance() + delta);
            pointsAccountRepository.save(account);

            PointsLedger ledger = new PointsLedger();
            ledger.setUserId(userId);
            ledger.setChangeAmount(delta);
            ledger.setReason(PointsLedger.Reason.ADJUSTMENT);
            ledger.setRelatedOrderId(null);
            ledger.setNote(note == null ? "Admin adjust +" : note);
            ledger.setBalanceAfter(account.getBalance());
            pointsLedgerRepository.save(ledger);
        } else {
            int need = -delta; // 要扣除的點數
            // 鎖可用批次並檢查
            List<PointLots> lots = pointLotsRepository.lockAvailableLotsOrderByExpiry(userId);
            int remainFromLots = lots.stream().mapToInt(l -> l.getEarnedPoints() - l.getUsedPoints()).sum();
            if (remainFromLots < need || account.getBalance() < need) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "可用點數不足");
            }
            int toConsume = need;
            for (PointLots lot : lots) {
                if (toConsume <= 0) break;
                int canUse = lot.getEarnedPoints() - lot.getUsedPoints();
                int use = Math.min(canUse, toConsume);
                lot.setUsedPoints(lot.getUsedPoints() + use);
                pointLotsRepository.save(lot);
                toConsume -= use;
            }
            account.setBalance(account.getBalance() - need);
            pointsAccountRepository.save(account);

            PointsLedger ledger = new PointsLedger();
            ledger.setUserId(userId);
            ledger.setChangeAmount(-need);
            ledger.setReason(PointsLedger.Reason.ADJUSTMENT);
            ledger.setRelatedOrderId(null);
            ledger.setNote(note == null ? "Admin adjust -" : note);
            ledger.setBalanceAfter(account.getBalance());
            pointsLedgerRepository.save(ledger);
        }
    }

    // 舊版接口保留，轉呼叫新版（不覆寫到期天數）
    public void adjustPoints(Long userId, int delta, String note) {
        adjustPoints(userId, delta, note, null);
    }

    // ======= 原有到期相關工具 =======
    /**
     * 檢查點數是否過期
     */
    public boolean isPointExpired(LocalDateTime earnedDate, PointRule rule) {
        if (rule.getExpiryPolicy() == PointRule.ExpiryPolicy.NONE) {
            return false;
        }
        if (rule.getExpiryPolicy() == PointRule.ExpiryPolicy.ROLLING_DAYS) {
            if (rule.getRollingDays() == null) {
                return false;
            }
            LocalDateTime expiryDate = earnedDate.plusDays(rule.getRollingDays());
            return LocalDateTime.now().isAfter(expiryDate);
        }
        if (rule.getExpiryPolicy() == PointRule.ExpiryPolicy.FIXED_DATE) {
            LocalDateTime expiryDate = getPointExpiryDate(earnedDate, rule);
            if (expiryDate == null) return false;
            return LocalDateTime.now().isAfter(expiryDate);
        }
        return false;
    }
    
    /**
     * 獲取點數過期日期
     */
    public LocalDateTime getPointExpiryDate(LocalDateTime earnedDate, PointRule rule) {
        if (rule.getExpiryPolicy() == PointRule.ExpiryPolicy.NONE) {
            return null;
        }
        if (rule.getExpiryPolicy() == PointRule.ExpiryPolicy.ROLLING_DAYS) {
            if (rule.getRollingDays() == null) {
                return null;
            }
            return earnedDate.plusDays(rule.getRollingDays());
        }
        if (rule.getExpiryPolicy() == PointRule.ExpiryPolicy.FIXED_DATE) {
            Integer mmdd = rule.getFixedExpireDay();
            MonthDay md = parseMonthDay(mmdd);
            if (md == null) return null;
            LocalDate earnedDateOnly = earnedDate.toLocalDate();
            LocalDate yearDate = md.atYear(earnedDateOnly.getYear());
            // 若獲點日已超過當年的到期日，則取隔年
            LocalDate target = !earnedDateOnly.isAfter(yearDate) ? yearDate : md.atYear(earnedDateOnly.getYear() + 1);
            // 設置為當日 23:59:59 以便在該日結束時過期
            return target.atTime(23, 59, 59);
        }
        return null;
    }

    private MonthDay parseMonthDay(Integer mmdd) {
        if (mmdd == null) return null;
        int val = mmdd;
        int mm = val / 100;
        int dd = val % 100;
        try {
            return MonthDay.of(mm, dd);
        } catch (Exception e) {
            return null;
        }
    }

    private PointsAccount newAccount(Long userId) {
        PointsAccount a = new PointsAccount();
        a.setUserId(userId);
        a.setBalance(0);
        a.setUpdatedAt(LocalDateTime.now());
        return a;
    }

    /**
     * 取得使用者在未來 days 天內將到期的剩餘點數總和（不含已用盡或無到期批次）。
     */
    public int getExpiringPoints(Long userId, int days) {
        if (userId == null || days <= 0) return 0;
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(days);
        Integer sum = pointLotsRepository.sumRemainingPointsExpiringBetween(userId, from, to);
        return sum == null ? 0 : sum;
    }
}
