package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointLots {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private Source source;
    
    @Column(name = "related_order_id")
    private Long relatedOrderId;
    
    @Column(name = "earned_points", nullable = false)
    private Integer earnedPoints;
    
    @Column(name = "used_points", nullable = false)
    private Integer usedPoints = 0;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum Source {
        ORDER, ADJUSTMENT, OTHER
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
