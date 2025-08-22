package com.example.bookstore.repository;

import com.example.bookstore.entity.PointReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PointReservationRepository extends JpaRepository<PointReservation, Long> {
    Optional<PointReservation> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PointReservation r WHERE r.orderId = :orderId")
    Optional<PointReservation> lockByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COALESCE(SUM(r.reservedPts), 0) FROM PointReservation r WHERE r.userId = :userId AND r.status = 'ACTIVE'")
    Integer sumActiveReservedByUser(@Param("userId") Long userId);
}
