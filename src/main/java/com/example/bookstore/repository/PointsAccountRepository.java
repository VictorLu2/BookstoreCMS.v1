package com.example.bookstore.repository;

import com.example.bookstore.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PointsAccount a WHERE a.userId = :userId")
    Optional<PointsAccount> lockByUserId(@Param("userId") Long userId);

    long countByBalanceGreaterThan(Integer balance);
}
