package com.example.bookstore.repository;

import com.example.bookstore.entity.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    
    List<AdminLog> findByAdminId(Long adminId);
    
    List<AdminLog> findByAction(String action);
    
    List<AdminLog> findByTargetType(String targetType);
    
    @Query("SELECT l FROM AdminLog l WHERE l.action LIKE %:keyword% OR l.targetType LIKE %:keyword% OR l.details LIKE %:keyword%")
    List<AdminLog> findByKeyword(@Param("keyword") String keyword);

    @Query("select year(l.createdAt) as y, month(l.createdAt) as m, "+
           "sum(case when l.action like 'CREATE%' then 1 else 0 end) as cCreate, "+
           "sum(case when l.action like 'UPDATE%' then 1 else 0 end) as cUpdate, "+
           "sum(case when l.action like 'DELETE%' then 1 else 0 end) as cDelete "+
           "from AdminLog l where l.createdAt >= :from "+
           "group by year(l.createdAt), month(l.createdAt) order by year(l.createdAt), month(l.createdAt)")
    List<Object[]> findMonthlyActionCounts(@Param("from") LocalDateTime from);

    @Query("SELECT l FROM AdminLog l WHERE (:keyword IS NULL OR :keyword = '' OR l.action LIKE %:keyword% OR l.targetType LIKE %:keyword% OR l.details LIKE %:keyword%) " +
           "AND (:action IS NULL OR :action = '' OR l.action LIKE CONCAT(:action, '%'))")
    Page<AdminLog> search(@Param("keyword") String keyword,
                          @Param("action") String action,
                          Pageable pageable);

    @Query("SELECT COUNT(l) FROM AdminLog l WHERE (:keyword IS NULL OR :keyword = '' OR l.action LIKE %:keyword% OR l.targetType LIKE %:keyword% OR l.details LIKE %:keyword%) " +
           "AND (:action IS NULL OR :action = '' OR l.action LIKE CONCAT(:action, '%'))")
    long countByFilters(@Param("keyword") String keyword, @Param("action") String action);

    @Query("SELECT COUNT(l) FROM AdminLog l WHERE (:keyword IS NULL OR :keyword = '' OR l.action LIKE %:keyword% OR l.targetType LIKE %:keyword% OR l.details LIKE %:keyword%) " +
           "AND (:action IS NULL OR :action = '' OR l.action LIKE CONCAT(:action, '%')) " +
           "AND l.action LIKE CONCAT(:typePrefix, '%')")
    long countByTypePrefix(@Param("keyword") String keyword,
                           @Param("action") String action,
                           @Param("typePrefix") String typePrefix);
}
