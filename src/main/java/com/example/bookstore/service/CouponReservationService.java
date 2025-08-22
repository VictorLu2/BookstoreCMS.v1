package com.example.bookstore.service;

import com.example.bookstore.entity.Coupon;
import com.example.bookstore.entity.CouponReservation;
import com.example.bookstore.entity.CouponUsage;
import com.example.bookstore.repository.CouponRepository;
import com.example.bookstore.repository.CouponReservationRepository;
import com.example.bookstore.repository.CouponUsageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponReservationService {

    private final CouponRepository couponRepository;
    private final CouponReservationRepository reservationRepository;
    private final CouponUsageRepository couponUsageRepository;

    @PersistenceContext
    private EntityManager em;

    public CouponReservation reserveByCouponId(Long userId, Long couponId, Long orderId, int ttlSeconds) {
        // 以券行鎖，避免高併發時重複保留
        Coupon lockedCoupon = em.find(Coupon.class, couponId, LockModeType.PESSIMISTIC_WRITE);
        if (lockedCoupon == null) {
            throw new IllegalArgumentException("優惠券不存在");
        }
        // 基本可用性檢查
        if (lockedCoupon.getStatus() != Coupon.CouponStatus.ACTIVE) {
            throw new IllegalStateException("優惠券未啟用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(lockedCoupon.getStartsAt()) || now.isAfter(lockedCoupon.getEndsAt())) {
            throw new IllegalStateException("優惠券不在有效期間");
        }
        // 使用次數限制（只以已核銷紀錄為準，不以保留計入）
        if (lockedCoupon.getPerUserLimit() != null) {
            long usedByUser = couponUsageRepository.countByCoupon_IdAndUserId(couponId, userId);
            if (usedByUser >= lockedCoupon.getPerUserLimit()) {
                throw new IllegalStateException("已達個人使用上限");
            }
        }
        if (lockedCoupon.getTotalUsageLimit() != null) {
            long totalUsed = couponUsageRepository.countByCoupon_Id(couponId);
            if (totalUsed >= lockedCoupon.getTotalUsageLimit()) {
                throw new IllegalStateException("已達總使��上限");
            }
        }
        // 同一用戶+同一券，若已有有效保留則直接返回（冪等）
        Optional<CouponReservation> existed = reservationRepository.findActiveByUserAndCoupon(userId, couponId, now);
        if (existed.isPresent()) return existed.get();

        CouponReservation r = new CouponReservation();
        r.setCoupon(lockedCoupon);
        r.setUserId(userId);
        r.setOrderId(orderId);
        r.setReservedAt(now);
        r.setExpiresAt(now.plusSeconds(ttlSeconds <= 0 ? 600 : ttlSeconds));
        return reservationRepository.save(r);
    }

    public CouponReservation reserveByGenericCode(Long userId, String code, Long orderId, int ttlSeconds) {
        Coupon coupon = couponRepository.findByGenericCodeAndActive(code)
                .orElseThrow(() -> new IllegalArgumentException("優惠券代碼無效或未啟用"));
        return reserveByCouponId(userId, coupon.getId(), orderId, ttlSeconds);
    }

    public Optional<CouponReservation> getActiveByUserAndOrder(Long userId, Long orderId) {
        return reservationRepository.findActiveByUserAndOrder(userId, orderId, LocalDateTime.now());
    }

    public void commit(Long userId, Long orderId) {
        CouponReservation r = reservationRepository.findActiveByUserAndOrder(userId, orderId, LocalDateTime.now())
                .orElse(null);
        // 若已無有效保留，可能已提交過或過期，嘗試以 orderId 判斷是否已核銷過，達到冪等
        if (r == null) {
            if (couponUsageRepository.existsByOrderId(orderId)) return; // 已核銷視為成功
            throw new IllegalStateException("找不到此訂單的有效鎖券");
        }
        // 在同一交易中鎖券行，確認使用次數限制並記錄核銷
        Coupon lockedCoupon = em.find(Coupon.class, r.getCoupon().getId(), LockModeType.PESSIMISTIC_WRITE);
        if (lockedCoupon == null) throw new IllegalStateException("優惠券不存在");
        // 使用上限再檢查（避免競態）
        if (lockedCoupon.getPerUserLimit() != null) {
            long usedByUser = couponUsageRepository.countByCoupon_IdAndUserId(lockedCoupon.getId(), userId);
            if (usedByUser >= lockedCoupon.getPerUserLimit()) {
                throw new IllegalStateException("已達個人使用上限");
            }
        }
        if (lockedCoupon.getTotalUsageLimit() != null) {
            long totalUsed = couponUsageRepository.countByCoupon_Id(lockedCoupon.getId());
            if (totalUsed >= lockedCoupon.getTotalUsageLimit()) {
                throw new IllegalStateException("已達總使用上限");
            }
        }
        // 寫入核銷紀錄（以 orderId 唯一保證冪等）
        if (!couponUsageRepository.existsByOrderId(orderId)) {
            CouponUsage usage = new CouponUsage();
            usage.setCoupon(lockedCoupon);
            usage.setUserId(userId);
            usage.setOrderId(orderId);
            couponUsageRepository.save(usage);
        }
        // 釋放保留
        r.setReleasedAt(LocalDateTime.now());
        reservationRepository.save(r);
    }

    public void cancel(Long userId, Long orderId) {
        CouponReservation r = reservationRepository.findActiveByUserAndOrder(userId, orderId, LocalDateTime.now())
                .orElse(null);
        if (r != null) {
            r.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(r);
        }
    }
}
