package com.example.bookstore.repository;

import com.example.bookstore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNo(String orderNo);
    
    List<Order> findByUser_UserId(Long userId);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.user.email LIKE %:keyword% OR o.orderNo LIKE %:keyword%")
    List<Order> findByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") Order.OrderStatus status, 
                                   @Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE (:keyword IS NULL OR :keyword = '' OR o.orderNo LIKE %:keyword% OR o.user.email LIKE %:keyword%) " +
           "AND (:status IS NULL OR o.status = :status)")
    Page<Order> searchForAdmin(@Param("keyword") String keyword,
                               @Param("status") Order.OrderStatus status,
                               Pageable pageable);
}
