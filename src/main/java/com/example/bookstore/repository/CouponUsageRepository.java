package com.example.bookstore.repository;

import com.example.bookstore.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCoupon_Id(Long couponId);
    long countByCoupon_IdAndUserId(Long couponId, Long userId);
    boolean existsByOrderId(Long orderId);
}

