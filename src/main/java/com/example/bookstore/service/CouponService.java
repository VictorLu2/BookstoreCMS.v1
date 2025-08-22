package com.example.bookstore.service;

import com.example.bookstore.entity.Coupon;
import com.example.bookstore.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {
    
    private final CouponRepository couponRepository;
    
    /**
     * 創建新優惠券
     */
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        return couponRepository.save(coupon);
    }
    
    /**
     * 更新優惠券
     */
    public Coupon updateCoupon(Long id, Coupon couponDetails) {
        Coupon coupon = couponRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("優惠券不存在"));
        
        if (couponDetails.getName() != null) {
            coupon.setName(couponDetails.getName());
        }
        if (couponDetails.getGenericCode() != null) {
            coupon.setGenericCode(couponDetails.getGenericCode());
        }
        if (couponDetails.getDiscountType() != null) {
            coupon.setDiscountType(couponDetails.getDiscountType());
        }
        if (couponDetails.getDiscountValue() != null) {
            coupon.setDiscountValue(couponDetails.getDiscountValue());
        }
        if (couponDetails.getMaxDiscountAmount() != null) {
            coupon.setMaxDiscountAmount(couponDetails.getMaxDiscountAmount());
        }
        if (couponDetails.getMinSpendAmount() != null) {
            coupon.setMinSpendAmount(couponDetails.getMinSpendAmount());
        }
        if (couponDetails.getStartsAt() != null) {
            coupon.setStartsAt(couponDetails.getStartsAt());
        }
        if (couponDetails.getEndsAt() != null) {
            coupon.setEndsAt(couponDetails.getEndsAt());
        }
        if (couponDetails.getStatus() != null) {
            coupon.setStatus(couponDetails.getStatus());
        }
        if (couponDetails.getTotalUsageLimit() != null) {
            coupon.setTotalUsageLimit(couponDetails.getTotalUsageLimit());
        }
        if (couponDetails.getPerUserLimit() != null) {
            coupon.setPerUserLimit(couponDetails.getPerUserLimit());
        }
        
        coupon.setUpdatedAt(LocalDateTime.now());
        return couponRepository.save(coupon);
    }
    
    /**
     * 刪除優惠券
     */
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("優惠券不存在"));
        couponRepository.delete(coupon);
    }
    
    /**
     * 獲取所有優惠券
     */
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }
    
    /**
     * 根據狀態獲取優惠券
     */
    public List<Coupon> getCouponsByStatus(Coupon.CouponStatus status) {
        return couponRepository.findByStatus(status);
    }
    
    /**
     * 搜尋優惠券
     */
    public List<Coupon> searchCoupons(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCoupons();
        }
        return couponRepository.findByKeyword(keyword.trim());
    }
    
    /**
     * 獲取活躍優���券
     */
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findActiveCoupons(LocalDateTime.now());
    }
    
    /**
     * 根據通用代碼獲取活躍優惠券
     */
    public Optional<Coupon> getActiveCouponByCode(String code) {
        return couponRepository.findByGenericCodeAndActive(code);
    }
    
    /**
     * 根據ID獲取優惠券
     */
    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }
    
    /**
     * 計算優惠券折扣金額
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        if (coupon.getMinSpendAmount() != null &&
            orderAmount.compareTo(BigDecimal.valueOf(coupon.getMinSpendAmount())) < 0) {
            return BigDecimal.ZERO; // 未達最低消費
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
            // 百分比折扣（discountValue 為整數百分比）
            discount = orderAmount
                    .multiply(BigDecimal.valueOf(coupon.getDiscountValue()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (coupon.getDiscountType() == Coupon.DiscountType.FIXED) {
            // 固定金額折扣（discountValue 為整數金額）
            discount = BigDecimal.valueOf(coupon.getDiscountValue());
        }
        
        // 檢查最大折扣金額
        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
        
        return discount;
    }
    
    /**
     * 檢查優惠券是否可用
     */
    public boolean isCouponValid(Coupon coupon, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        
        // 檢查狀態
        if (coupon.getStatus() != Coupon.CouponStatus.ACTIVE) {
            return false;
        }
        
        // 檢查時間
        if (now.isBefore(coupon.getStartsAt()) || now.isAfter(coupon.getEndsAt())) {
            return false;
        }
        
        // 檢查最低消費（minSpendAmount 為整數）
        if (coupon.getMinSpendAmount() != null &&
            orderAmount.compareTo(BigDecimal.valueOf(coupon.getMinSpendAmount())) < 0) {
            return false;
        }
        
        return true;
    }

    /**
     * 後端分頁查詢方法，支援關鍵字與狀態過濾
     */
    public Page<Coupon> searchCoupons(String keyword, Coupon.CouponStatus status, Pageable pageable) {
        return couponRepository.searchForAdmin(keyword, status, pageable);
    }
}
