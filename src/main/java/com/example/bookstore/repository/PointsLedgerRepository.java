package com.example.bookstore.repository;

import com.example.bookstore.entity.PointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {
    List<PointsLedger> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(l.changeAmount), 0) FROM PointsLedger l WHERE l.changeAmount > 0 AND l.reason IN :reasons")
    Integer sumIssuedPoints(@Param("reasons") List<PointsLedger.Reason> reasons);
}
