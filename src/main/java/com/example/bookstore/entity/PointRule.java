package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "point_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reward_rate_bp", nullable = false)
    private Integer rewardRateBp = 100; // 萬分比，100 = 1%
    
    @Column(name = "max_reward_points")
    private Integer maxRewardPoints;
    
    // 將兌換率改為整數（元/點）
    @Column(name = "redeem_rate", nullable = false)
    private Integer redeemRate = 1;

    @Column(name = "max_redeem_ratio_bp")
    private Integer maxRedeemRatioBp; // 萬分比，5000 = 50%
    
    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_policy", nullable = false)
    private ExpiryPolicy expiryPolicy = ExpiryPolicy.ROLLING_DAYS;
    
    @Column(name = "rolling_days")
    private Integer rollingDays = 180;
    
    @Column(name = "fixed_expire_day")
    private Integer fixedExpireDay;
    
    public enum ExpiryPolicy {
        NONE, FIXED_DATE, ROLLING_DAYS
    }
}
