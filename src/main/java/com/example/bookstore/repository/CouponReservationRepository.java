package com.example.bookstore.repository;

import com.example.bookstore.entity.CouponReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CouponReservationRepository extends JpaRepository<CouponReservation, Long> {

    @Query("SELECT r FROM CouponReservation r WHERE r.userId = :userId AND r.coupon.id = :couponId AND r.releasedAt IS NULL AND r.expiresAt > :now")
    Optional<CouponReservation> findActiveByUserAndCoupon(@Param("userId") Long userId,
                                                          @Param("couponId") Long couponId,
                                                          @Param("now") LocalDateTime now);

    @Query("SELECT r FROM CouponReservation r WHERE r.userId = :userId AND r.orderId = :orderId AND r.releasedAt IS NULL AND r.expiresAt > :now")
    Optional<CouponReservation> findActiveByUserAndOrder(@Param("userId") Long userId,
                                                         @Param("orderId") Long orderId,
                                                         @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE CouponReservation r SET r.releasedAt = :now WHERE r.id = :id AND r.releasedAt IS NULL")
    int softReleaseById(@Param("id") Long id, @Param("now") LocalDateTime now);
}

