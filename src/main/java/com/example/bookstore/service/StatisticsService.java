package com.example.bookstore.service;

import com.example.bookstore.entity.Order;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * 獲取銷售金額統計（金額單位：分）
     */
    public Map<String, Object> getSalesStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findByDateRange(startDate, endDate);
        
        // 僅統計已付款與已完成訂單（PAID、FULFILLED）
        int totalSales = orders.stream()
            .filter(order -> order.getStatus() == Order.OrderStatus.PAID || order.getStatus() == Order.OrderStatus.FULFILLED)
            .map(Order::getPayableAmount)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();

        int totalDiscount = orders.stream()
            .filter(order -> order.getStatus() == Order.OrderStatus.PAID || order.getStatus() == Order.OrderStatus.FULFILLED)
            .map(Order::getDiscountAmount)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSales", totalSales);
        stats.put("totalDiscount", totalDiscount);
        stats.put("netSales", totalSales + totalDiscount);
        stats.put("orderCount", orders.size());
        stats.put("paidOrderCount", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PAID).count());

        return stats;
    }
    
    /**
     * 獲取每日銷售趨勢（金額單位：分）
     */
    public List<Map<String, Object>> getDailySalesTrend(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findByDateRange(startDate, endDate);
        
        // 同時計入已付款與已完成訂單（PAID、FULFILLED）
        Map<LocalDate, Integer> dailySales = orders.stream()
            .filter(order -> order.getStatus() == Order.OrderStatus.PAID || order.getStatus() == Order.OrderStatus.FULFILLED)
            .collect(Collectors.groupingBy(
                order -> order.getCreatedAt().toLocalDate(),
                Collectors.summingInt(o -> Optional.ofNullable(o.getPayableAmount()).orElse(0))
            ));
        
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        
        while (!current.isAfter(end)) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", current.toString());
            dayData.put("sales", dailySales.getOrDefault(current, 0));
            trend.add(dayData);
            current = current.plusDays(1);
        }
        
        return trend;
    }
    
    /**
     * 獲取訂單狀態統計
     */
    public Map<String, Long> getOrderStatusStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> statusStats = new HashMap<>();
        
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            Long count = orderRepository.countByStatusAndDateRange(status, startDate, endDate);
            statusStats.put(status.name(), count);
        }
        
        return statusStats;
    }
    
    /**
     * 獲取熱銷商品排行榜
     */
    public List<Map<String, Object>> getTopSellingBooks(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        // 這裡需��實作更複雜的邏輯來統計書籍銷售量
        // 暫時返回空列表，可以根據需求實作
        return new ArrayList<>();
    }
    
    /**
     * 獲取月度統計
     */
    public Map<String, Object> getMonthlyStatistics(int year, int month) {
        try {
            LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);

            return getSalesStatistics(startDate, endDate);
        } catch (Exception e) {
            // 如果查詢失敗，返回默認統計值（整數分）
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalSales", 0);
            defaultStats.put("totalDiscount", 0);
            defaultStats.put("netSales", 0);
            defaultStats.put("orderCount", 0L);
            defaultStats.put("paidOrderCount", 0L);
            return defaultStats;
        }
    }

    /**
     * 獲取年度統���
     */
    public Map<String, Object> getYearlyStatistics(int year) {
        try {
            LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            LocalDateTime endDate = startDate.plusYears(1).minusSeconds(1);

            return getSalesStatistics(startDate, endDate);
        } catch (Exception e) {
            // 如果查詢失敗，返回默認統計值（整數分）
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalSales", 0);
            defaultStats.put("totalDiscount", 0);
            defaultStats.put("netSales", 0);
            defaultStats.put("orderCount", 0L);
            defaultStats.put("paidOrderCount", 0L);
            return defaultStats;
        }
    }

    /**
     * 獲取使用者統計（只計算一般會員，排除管理員）
     */
    public Map<String, Object> getUserStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            long totalUsers = userRepository.countAllRegularUsers();
            long activeUsers = userRepository.countActiveRegularUsers();
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
            long newUsersThisMonth = userRepository.countRegularUsersCreatedBetween(startOfMonth, endOfMonth);

            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("newUsersThisMonth", newUsersThisMonth);
            return stats;
        } catch (Exception e) {
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalUsers", 0L);
            defaultStats.put("activeUsers", 0L);
            defaultStats.put("newUsersThisMonth", 0L);
            return defaultStats;
        }
    }
}
