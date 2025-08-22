package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsLedger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private Reason reason;
    
    @Column(name = "related_order_id")
    private Long relatedOrderId;
    
    @Column(name = "note")
    private String note;
    
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum Reason {
        PURCHASE_REWARD, REDEEM, ADJUSTMENT, EXPIRE, REFUND
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
