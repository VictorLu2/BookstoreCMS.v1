package com.example.bookstore.controller;

import com.example.bookstore.entity.PointReservation;
import com.example.bookstore.entity.PointsAccount;
import com.example.bookstore.entity.PointsLedger;
import com.example.bookstore.entity.PointLots;
import com.example.bookstore.entity.PointRule;
import com.example.bookstore.repository.PointReservationRepository;
import com.example.bookstore.repository.PointsAccountRepository;
import com.example.bookstore.repository.PointsLedgerRepository;
import com.example.bookstore.repository.PointLotsRepository;
import com.example.bookstore.service.PointService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointService pointService;
    private final PointsAccountRepository pointsAccountRepository;
    private final PointReservationRepository pointReservationRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final PointLotsRepository pointLotsRepository;

    // 查詢可用點數（餘額、已預留、可用）
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getAvailable(@PathVariable Long userId) {
        PointsAccount acc = pointsAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointsAccount a = new PointsAccount();
                    a.setUserId(userId);
                    a.setBalance(0);
                    return a;
                });
        Integer reserved = pointReservationRepository.sumActiveReservedByUser(userId);
        int available = pointService.getAvailablePoints(userId);
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("balance", acc.getBalance());
        body.put("reserved", reserved == null ? 0 : reserved);
        body.put("available", available);
        return ResponseEntity.ok(body);
    }

    // 預留點數
    @PostMapping("/reserve")
    public ResponseEntity<PointReservation> reserve(@RequestBody ReserveRequest req) {
        PointReservation r = pointService.reservePoints(req.getUserId(), req.getOrderId(), req.getPoints());
        return ResponseEntity.ok(r);
    }

    // 確認扣點
    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commit(@RequestBody CommitRequest req) {
        pointService.commitReservation(req.getUserId(), req.getOrderId());
        Map<String, Object> body = Map.of("status", "ok");
        return ResponseEntity.ok(body);
    }

    // 取消預留
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@RequestBody CommitRequest req) {
        pointService.cancelReservation(req.getUserId(), req.getOrderId());
        Map<String, Object> body = Map.of("status", "ok");
        return ResponseEntity.ok(body);
    }

    // 計算回饋點數（amount: 金額；unit: yuan|cent，預設 cent）
    @GetMapping("/calculate-reward")
    public ResponseEntity<Map<String, Object>> calcReward(@RequestParam("amount") Long amount,
                                                          @RequestParam(value = "unit", required = false, defaultValue = "cent") String unit) {
        BigDecimal amtYuan = "yuan".equalsIgnoreCase(unit)
                ? BigDecimal.valueOf(amount)
                : BigDecimal.valueOf(amount).movePointLeft(2);
        int points = pointService.calculateOrderPoints(amtYuan);
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("unit", unit);
        body.put("points", points);
        return ResponseEntity.ok(body);
    }

    // （可選）回饋入帳（直接加點）
    @PostMapping("/grant")
    public ResponseEntity<Map<String, Object>> grant(@RequestBody GrantRequest req) {
        pointService.grantPoints(req.getUserId(), req.getOrderId(), req.getPoints(), req.getNote());
        Map<String, Object> body = Map.of("status", "ok");
        return ResponseEntity.ok(body);
    }

    // （可選）點數流水
    @GetMapping("/{userId}/ledger")
    public ResponseEntity<List<PointsLedger>> ledger(@PathVariable Long userId) {
        return ResponseEntity.ok(pointsLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    // 取得使用者可用批次（含到期資訊）
    @GetMapping("/{userId}/lots")
    public ResponseEntity<List<LotVM>> lots(@PathVariable Long userId) {
        List<PointLots> lots = pointLotsRepository.findAvailableLotsOrderByExpiry(userId);
        List<LotVM> body = lots.stream().map(l -> new LotVM(
                l.getId(),
                l.getSource().name(),
                l.getRelatedOrderId(),
                l.getEarnedPoints(),
                l.getUsedPoints(),
                l.getEarnedPoints() - l.getUsedPoints(),
                l.getExpiresAt(),
                l.getCreatedAt()
        )).toList();
        return ResponseEntity.ok(body);
    }

    // 取得使用者在未來 days 天內將到期的點數總和
    @GetMapping("/{userId}/expiring")
    public ResponseEntity<Map<String, Object>> expiring(@PathVariable Long userId,
                                                        @RequestParam(value = "days", defaultValue = "30") Integer days) {
        int v = pointService.getExpiringPoints(userId, days == null ? 30 : days);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("days", days == null ? 30 : days);
        body.put("expiring", v);
        return ResponseEntity.ok(body);
    }

    // 取得目前點數規則（轉為易於前端使用的格式）
    @GetMapping("/rule")
    public ResponseEntity<Map<String, Object>> rule() {
        PointRule r = pointService.getCurrentPointRule();
        if (r == null) return ResponseEntity.ok(Map.of());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rewardRatePercent", r.getRewardRateBp() == null ? null : r.getRewardRateBp() / 100);
        body.put("maxRewardPoints", r.getMaxRewardPoints());
        body.put("redeemRate", r.getRedeemRate()); // 元/點
        body.put("maxRedeemRatioPercent", r.getMaxRedeemRatioBp() == null ? null : r.getMaxRedeemRatioBp() / 100);
        body.put("expiryPolicy", r.getExpiryPolicy().name());
        body.put("rollingDays", r.getRollingDays());
        return ResponseEntity.ok(body);
    }

    // 新增：折抵金額試算 API
    @GetMapping("/calculate-deduction")
    public ResponseEntity<Map<String, Object>> calcDeduction(@RequestParam("amount") Long amount,
                                                             @RequestParam(value = "unit", defaultValue = "cent") String unit,
                                                             @RequestParam("points") Integer points) {
        if (points == null || points <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "points 必須 > 0"));
        }
        BigDecimal amtYuan = "yuan".equalsIgnoreCase(unit)
                ? BigDecimal.valueOf(amount)
                : BigDecimal.valueOf(amount).movePointLeft(2);
        BigDecimal deduction = pointService.calculatePointDeduction(points, amtYuan);
        int deductionCents = deduction.movePointRight(2).setScale(0, java.math.RoundingMode.DOWN).intValue();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("unit", unit);
        body.put("points", points);
        body.put("deductionYuan", deduction);
        body.put("deductionCents", deductionCents);
        return ResponseEntity.ok(body);
    }

    // 新增：計算本筆最大可用點數（考量比例上限、兌換率與使用者可用點）
    @GetMapping("/max-redeem")
    public ResponseEntity<Map<String, Object>> maxRedeem(@RequestParam("userId") Long userId,
                                                         @RequestParam("amount") Long amount,
                                                         @RequestParam(value = "unit", defaultValue = "cent") String unit) {
        PointRule rule = pointService.getCurrentPointRule();
        if (rule == null) return ResponseEntity.ok(Map.of());
        int available = pointService.getAvailablePoints(userId);
        BigDecimal amtYuan = "yuan".equalsIgnoreCase(unit)
                ? BigDecimal.valueOf(amount)
                : BigDecimal.valueOf(amount).movePointLeft(2);
        // 以比例上限換算最大可折抵金額
        BigDecimal ratioCap = null;
        if (rule.getMaxRedeemRatioBp() != null) {
            ratioCap = amtYuan.multiply(BigDecimal.valueOf(rule.getMaxRedeemRatioBp()))
                    .divide(BigDecimal.valueOf(10000), 2, java.math.RoundingMode.DOWN);
        }
        // 用兌換率（元/點）換算���限點數
        int pointsByRatio = Integer.MAX_VALUE;
        if (ratioCap != null) {
            pointsByRatio = ratioCap.divide(BigDecimal.valueOf(rule.getRedeemRate()), 0, java.math.RoundingMode.DOWN).intValue();
        }
        int maxUsablePoints = Math.max(0, Math.min(available, pointsByRatio));
        BigDecimal maxDeduction = pointService.calculatePointDeduction(maxUsablePoints, amtYuan);
        int maxDeductionCents = maxDeduction.movePointRight(2).setScale(0, java.math.RoundingMode.DOWN).intValue();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("unit", unit);
        body.put("availablePoints", available);
        body.put("redeemRateYuanPerPoint", rule.getRedeemRate());
        body.put("maxRedeemRatioBp", rule.getMaxRedeemRatioBp());
        body.put("maxUsablePoints", maxUsablePoints);
        body.put("maxDeductionYuan", maxDeduction);
        body.put("maxDeductionCents", maxDeductionCents);
        return ResponseEntity.ok(body);
    }

    @Data
    public static class ReserveRequest {
        private Long userId;
        private Long orderId;
        private Integer points;
    }

    @Data
    public static class CommitRequest {
        private Long userId;
        private Long orderId;
    }

    @Data
    public static class GrantRequest {
        private Long userId;
        private Long orderId;
        private Integer points;
        private String note;
    }

    // 移除 Lombok 建構子註解，改為手寫建構子與 getter，避免編譯找不到 AllArgsConstructor
    public static class LotVM {
        private Long id;
        private String source;
        private Long relatedOrderId;
        private Integer earnedPoints;
        private Integer usedPoints;
        private Integer remainingPoints;
        private java.time.LocalDateTime expiresAt;
        private java.time.LocalDateTime createdAt;

        public LotVM() {}

        public LotVM(Long id, String source, Long relatedOrderId, Integer earnedPoints, Integer usedPoints,
                     Integer remainingPoints, java.time.LocalDateTime expiresAt, java.time.LocalDateTime createdAt) {
            this.id = id;
            this.source = source;
            this.relatedOrderId = relatedOrderId;
            this.earnedPoints = earnedPoints;
            this.usedPoints = usedPoints;
            this.remainingPoints = remainingPoints;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getSource() { return source; }
        public Long getRelatedOrderId() { return relatedOrderId; }
        public Integer getEarnedPoints() { return earnedPoints; }
        public Integer getUsedPoints() { return usedPoints; }
        public Integer getRemainingPoints() { return remainingPoints; }
        public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    }
}
