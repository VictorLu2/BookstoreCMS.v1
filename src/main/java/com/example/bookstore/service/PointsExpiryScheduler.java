package com.example.bookstore.service;

import com.example.bookstore.entity.PointLots;
import com.example.bookstore.entity.PointsAccount;
import com.example.bookstore.entity.PointsLedger;
import com.example.bookstore.repository.PointLotsRepository;
import com.example.bookstore.repository.PointsAccountRepository;
import com.example.bookstore.repository.PointsLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsExpiryScheduler {

    private final PointLotsRepository pointLotsRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsLedgerRepository pointsLedgerRepository;

    // 每天 02:30 執行一次過期處理（可依需求調整頻率）
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void expireLotsDaily() {
        LocalDateTime now = LocalDateTime.now();
        List<PointLots> expiredLots = pointLotsRepository.lockExpiredLots(now);
        if (expiredLots.isEmpty()) return;

        int totalExpired = 0;
        for (PointLots lot : expiredLots) {
            int remaining = lot.getEarnedPoints() - lot.getUsedPoints();
            if (remaining <= 0) continue;

            // 標記此批次為全數用罄（由於到期）
            lot.setUsedPoints(lot.getEarnedPoints());
            pointLotsRepository.save(lot);

            // 扣帳戶餘額（保底不小於 0）
            PointsAccount acc = pointsAccountRepository.lockByUserId(lot.getUserId())
                    .orElseGet(() -> {
                        PointsAccount a = new PointsAccount();
                        a.setUserId(lot.getUserId());
                        a.setBalance(0);
                        a.setUpdatedAt(now);
                        return pointsAccountRepository.save(a);
                    });
            int newBalance = Math.max(0, acc.getBalance() - remaining);
            acc.setBalance(newBalance);
            pointsAccountRepository.save(acc);

            // 記一筆過期流水
            PointsLedger ledger = new PointsLedger();
            ledger.setUserId(lot.getUserId());
            ledger.setChangeAmount(-remaining);
            ledger.setReason(PointsLedger.Reason.EXPIRE);
            ledger.setRelatedOrderId(lot.getRelatedOrderId());
            ledger.setNote("Points expired");
            ledger.setBalanceAfter(newBalance);
            pointsLedgerRepository.save(ledger);

            totalExpired += remaining;
        }
        log.info("Points expiry processed at {}. Lots: {}, Total expired points: {}", now, expiredLots.size(), totalExpired);
    }
}

