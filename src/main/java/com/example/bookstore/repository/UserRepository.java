package com.example.bookstore.repository;

import com.example.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.enabled = :enabled")
    List<User> findByEnabled(@Param("enabled") Boolean enabled);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE u.role.roleName = 'ROLE_USER'")
    List<User> findAllUsers();

    // 統計：一般會員（排除管理員）
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = 'ROLE_USER'")
    long countAllRegularUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = 'ROLE_USER' AND u.enabled = true")
    long countActiveRegularUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = 'ROLE_USER' AND u.createdAt BETWEEN :start AND :end")
    long countRegularUsersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 其他統計（保留）
    long countByEnabled(Boolean enabled);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT u FROM User u WHERE u.role.roleName = 'ROLE_USER' AND (:keyword IS NULL OR :keyword = '' OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%) AND (:status IS NULL OR u.enabled = :status)")
    org.springframework.data.domain.Page<User> searchUsersForAdmin(@Param("keyword") String keyword,
                                                                   @Param("status") Boolean status,
                                                                   org.springframework.data.domain.Pageable pageable);
}
