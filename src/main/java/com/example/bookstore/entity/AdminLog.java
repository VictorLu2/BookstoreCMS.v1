package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 對齊資料庫：允許 NULL，以配合 ON DELETE SET NULL 外鍵策略
    @Column(name = "admin_id")
    private Long adminId;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "target_id")
    private Long targetId;
    
    @Column(name = "target_type")
    private String targetType;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
