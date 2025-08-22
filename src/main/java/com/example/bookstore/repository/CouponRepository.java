package com.example.bookstore.repository;

import com.example.bookstore.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    List<Coupon> findByStatus(Coupon.CouponStatus status);
    
    @Query("SELECT c FROM Coupon c WHERE c.startsAt <= :now AND c.endsAt >= :now AND c.status = 'ACTIVE'")
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Coupon c WHERE c.genericCode = :code AND c.status = 'ACTIVE'")
    Optional<Coupon> findByGenericCodeAndActive(@Param("code") String code);
    
    @Query("SELECT c FROM Coupon c WHERE c.name LIKE %:keyword% OR c.genericCode LIKE %:keyword%")
    List<Coupon> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c FROM Coupon c WHERE (:keyword IS NULL OR :keyword = '' OR c.name LIKE %:keyword% OR c.genericCode LIKE %:keyword%) " +
           "AND (:status IS NULL OR c.status = :status)")
    Page<Coupon> searchForAdmin(@Param("keyword") String keyword,
                                @Param("status") Coupon.CouponStatus status,
                                Pageable pageable);
}
