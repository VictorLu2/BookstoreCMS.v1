package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderItem;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final CouponReservationService couponReservationService;
    private final PointService pointService;

    /**
     * 獲取所有訂單
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * 根據ID獲取訂單
     */
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * 根據訂單號獲取訂單
     */
    public Optional<Order> getOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    /**
     * 根據使用者ID獲取訂單
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUser_UserId(userId);
    }

    /**
     * 根據狀態獲取訂單
     */
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * 根據日期範圍獲取訂單
     */
    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByDateRange(startDate, endDate);
    }

    /**
     * 搜尋訂單
     */
    public List<Order> searchOrders(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllOrders();
        }
        return orderRepository.findByKeyword(keyword.trim());
    }

    /**
     * 更新訂單狀態
     */
    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "訂單不存在"));
        order.setStatus(status);

        if (status == Order.OrderStatus.PAID) {
            order.setPaidAt(LocalDateTime.now());
            try {
                couponReservationService.commit(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
            // 新增：點數預留轉為實扣
            try {
                pointService.commitReservation(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
            // 自動回饋點數（以應付金額為基礎，單位：分 -> 元）
            try {
                Integer payableCents = order.getPayableAmount();
                if (payableCents != null && payableCents > 0) {
                    BigDecimal payableYuan = BigDecimal.valueOf(payableCents).movePointLeft(2);
                    int reward = pointService.calculateOrderPoints(payableYuan);
                    if (reward > 0) {
                        pointService.grantPoints(order.getUser().getUserId(), order.getId(), reward, "Auto reward on paid");
                    }
                }
            } catch (Exception ignored) {}
        } else if (status == Order.OrderStatus.FAILED) {
            try {
                couponReservationService.cancel(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
            // ���增：取消點數預留
            try {
                pointService.cancelReservation(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
        }

        return orderRepository.save(order);
    }

    // 依訂單編號更新狀態
    public Order updateOrderStatusByOrderNo(String orderNo, Order.OrderStatus status) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "訂單不存在: " + orderNo));
        order.setStatus(status);
        if (status == Order.OrderStatus.PAID) {
            order.setPaidAt(LocalDateTime.now());
            try {
                couponReservationService.commit(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
            // 新增：點數預留轉為實扣
            try {
                pointService.commitReservation(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
            // 自動回饋點數
            try {
                Integer payableCents = order.getPayableAmount();
                if (payableCents != null && payableCents > 0) {
                    BigDecimal payableYuan = BigDecimal.valueOf(payableCents).movePointLeft(2);
                    int reward = pointService.calculateOrderPoints(payableYuan);
                    if (reward > 0) {
                        pointService.grantPoints(order.getUser().getUserId(), order.getId(), reward, "Auto reward on paid");
                    }
                }
            } catch (Exception ignored) {}
        } else if (status == Order.OrderStatus.FAILED) {
            try {
                couponReservationService.cancel(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
            // 新增：取消點數預留
            try {
                pointService.cancelReservation(order.getUser().getUserId(), order.getId());
            } catch (Exception ignored) {}
        }
        return orderRepository.save(order);
    }

    /**
     * 獲取訂單統計
     */
    public Long getOrderCountByStatusAndDateRange(Order.OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.countByStatusAndDateRange(status, startDate, endDate);
    }

    /**
     * 獲取指定會員的歷史訂單
     */
    public List<Order> getUserOrderHistory(Long userId) {
        return orderRepository.findByUser_UserId(userId);
    }

    /**
     * 建立訂單
     */
    public Order createOrder(Order order) {
        // 訂單編號：若��提供則產生；若提供但已存在則回 409
        if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            order.setOrderNo(generateUniqueOrderNo());
        } else if (orderRepository.findByOrderNo(order.getOrderNo()).isPresent()) {
            throw new IllegalArgumentException("訂單編號已存在: " + order.getOrderNo());
        }

        // 預設狀態
        order.setStatus(Order.OrderStatus.CREATED);

        // 確保必要欄位
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }

        // 綁定訂單明細關聯 (允許以 bookId 或 既有 Book.id 指定)
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                item.setOrder(order);

                Long bookId = null;
                if (item.getBook() != null) {
                    bookId = item.getBook().getId();
                }
                if (bookId == null) {
                    bookId = item.getBookId();
                }
                if (bookId == null) {
                    throw new IllegalArgumentException("OrderItem 必須提供既有的 bookId");
                }

                final Long fBookId = bookId;
                Book managedBook = bookRepository.findById(fBookId)
                        .orElseThrow(() -> new IllegalArgumentException("書籍不存在 ID: " + fBookId));
                item.setBook(managedBook);

                // 若快照欄位未提供，使用書籍目前資料補齊
                if (item.getNameSnapshot() == null) {
                    item.setNameSnapshot(managedBook.getTitle());
                }
                if (item.getUnitPriceSnapshot() == null && managedBook.getListPrice() != null) {
                    item.setUnitPriceSnapshot(new BigDecimal(managedBook.getListPrice()));
                }
            }
        }

        return orderRepository.save(order);
    }

    private String generateUniqueOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String prefix = "O-" + date + "-";
        for (int i = 0; i < 5; i++) {
            String seq = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
            String candidate = prefix + seq;
            if (orderRepository.findByOrderNo(candidate).isEmpty()) {
                return candidate;
            }
        }
        // 後備：極小機率仍撞號時使用時間戳
        return "O-" + System.currentTimeMillis();
    }

    public Page<Order> searchOrders(String keyword, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.searchForAdmin(keyword, status, pageable);
    }
}
