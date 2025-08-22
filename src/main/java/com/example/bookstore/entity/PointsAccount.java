package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsAccount {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "balance", nullable = false)
    private Integer balance = 0;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
