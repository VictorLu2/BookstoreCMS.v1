package com.example.bookstore.controller;

import com.example.bookstore.entity.CouponReservation;
import com.example.bookstore.entity.Coupon;
import com.example.bookstore.service.CouponReservationService;
import com.example.bookstore.service.CouponService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponReservationService reservationService;
    private final CouponService couponService;

    @PostMapping("/reserve/code")
    public ResponseEntity<CouponReservation> reserveByCode(@RequestBody ReserveByCodeRequest req) {
        CouponReservation r = reservationService.reserveByGenericCode(req.getUserId(), req.getCode(), req.getOrderId(), req.getTtlSeconds() == null ? 600 : req.getTtlSeconds());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/reserve/id")
    public ResponseEntity<CouponReservation> reserveById(@RequestBody ReserveByIdRequest req) {
        CouponReservation r = reservationService.reserveByCouponId(req.getUserId(), req.getCouponId(), req.getOrderId(), req.getTtlSeconds() == null ? 600 : req.getTtlSeconds());
        return ResponseEntity.ok(r);
    }

    @GetMapping("/active")
    public ResponseEntity<CouponReservation> getActive(@RequestParam Long userId, @RequestParam Long orderId) {
        return reservationService.getActiveByUserAndOrder(userId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commit(@RequestBody BasicRequest req) {
        reservationService.commit(req.getUserId(), req.getOrderId());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@RequestBody BasicRequest req) {
        reservationService.cancel(req.getUserId(), req.getOrderId());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // 新增：優惠券折扣預覽
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> preview(@RequestParam("code") String code,
                                                       @RequestParam("amount") Long amount,
                                                       @RequestParam(value = "unit", defaultValue = "cent") String unit) {
        Coupon coupon = couponService.getActiveCouponByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("優惠券代碼無效或未啟用"));
        BigDecimal amtYuan = "yuan".equalsIgnoreCase(unit)
                ? BigDecimal.valueOf(amount)
                : BigDecimal.valueOf(amount).movePointLeft(2);
        boolean valid = couponService.isCouponValid(coupon, amtYuan);
        BigDecimal discountYuan = valid ? couponService.calculateDiscount(coupon, amtYuan) : BigDecimal.ZERO;
        int discountCents = discountYuan.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        Map<String, Object> body = Map.of(
                "valid", valid,
                "couponId", coupon.getId(),
                "name", coupon.getName(),
                "discountType", coupon.getDiscountType().name(),
                "discountValue", coupon.getDiscountValue(),
                "maxDiscountAmountYuan", coupon.getMaxDiscountAmount(),
                "minSpendAmountYuan", coupon.getMinSpendAmount(),
                "discountYuan", discountYuan,
                "discountCents", discountCents
        );
        return ResponseEntity.ok(body);
    }

    @Data
    public static class ReserveByCodeRequest {
        private Long userId;
        private Long orderId;
        private String code;
        private Integer ttlSeconds;
    }

    @Data
    public static class ReserveByIdRequest {
        private Long userId;
        private Long orderId;
        private Long couponId;
        private Integer ttlSeconds;
    }

    @Data
    public static class BasicRequest {
        private Long userId;
        private Long orderId;
    }
}
