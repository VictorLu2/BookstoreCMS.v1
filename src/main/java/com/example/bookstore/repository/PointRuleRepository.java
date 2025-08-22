package com.example.bookstore.repository;

import com.example.bookstore.entity.PointRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRuleRepository extends JpaRepository<PointRule, Long> {
    
    PointRule findFirstByOrderByIdAsc();
}
