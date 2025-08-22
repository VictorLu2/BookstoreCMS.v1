package com.example.bookstore.repository;

import com.example.bookstore.entity.PointLots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface PointLotsRepository extends JpaRepository<PointLots, Long> {

    @Query("SELECT l FROM PointLots l WHERE l.userId = :userId AND l.usedPoints < l.earnedPoints ORDER BY CASE WHEN l.expiresAt IS NULL THEN 1 ELSE 0 END, l.expiresAt ASC, l.id ASC")
    List<PointLots> findAvailableLotsOrderByExpiry(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM PointLots l WHERE l.userId = :userId AND l.usedPoints < l.earnedPoints ORDER BY CASE WHEN l.expiresAt IS NULL THEN 1 ELSE 0 END, l.expiresAt ASC, l.id ASC")
    List<PointLots> lockAvailableLotsOrderByExpiry(@Param("userId") Long userId);

    @Query("SELECT l FROM PointLots l WHERE l.expiresAt IS NOT NULL AND l.expiresAt < :now AND l.usedPoints < l.earnedPoints")
    List<PointLots> findExpiredLots(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM PointLots l WHERE l.expiresAt IS NOT NULL AND l.expiresAt < :now AND l.usedPoints < l.earnedPoints")
    List<PointLots> lockExpiredLots(@Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(l.earnedPoints - l.usedPoints), 0) FROM PointLots l " +
           "WHERE l.userId = :userId AND l.expiresAt IS NOT NULL AND l.expiresAt BETWEEN :from AND :to " +
           "AND l.usedPoints < l.earnedPoints")
    Integer sumRemainingPointsExpiringBetween(@Param("userId") Long userId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(l.earnedPoints - l.usedPoints), 0) FROM PointLots l " +
           "WHERE l.expiresAt IS NOT NULL AND l.expiresAt BETWEEN :from AND :to " +
           "AND l.usedPoints < l.earnedPoints")
    Integer sumRemainingPointsExpiringBetweenAll(@Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
}
