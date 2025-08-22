package com.example.bookstore.controller;

import com.example.bookstore.entity.*;
import com.example.bookstore.service.*;
import com.example.bookstore.repository.PointsAccountRepository;
import com.example.bookstore.repository.PointReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    private final BookService bookService;
    private final OrderService orderService;
    private final PointService pointService;
    private final CouponService couponService;
    private final StatisticsService statisticsService;
    private final AdminLogService adminLogService;
    private final PointsAccountRepository pointsAccountRepository;
    private final PointReservationRepository pointReservationRepository;
    // 新增：注入點數相關 repository 以做統計（若已有可忽略）
    private final com.example.bookstore.repository.PointsLedgerRepository pointsLedgerRepository;
    private final com.example.bookstore.repository.PointLotsRepository pointLotsRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 獲取統計資料
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        Map<String, Object> monthlyStats = statisticsService.getSalesStatistics(startOfMonth, endOfMonth);
        Map<String, Object> userStats = statisticsService.getUserStatistics();
        long bookCount = bookService.countBooks();

        // 本月銷售額（不除以100，直接顯示原始整數）
        int monthlySalesCents = (int) monthlyStats.getOrDefault("totalSales", 0);
        int monthlySalesYuan = monthlySalesCents; // 不做換算

        // 銷售趨勢（本月每日）
        var trend = statisticsService.getDailySalesTrend(startOfMonth, endOfMonth);
        List<String> salesLabels = new ArrayList<>();
        List<Integer> salesData = new ArrayList<>();
        for (Map<String, Object> day : trend) {
            salesLabels.add((String) day.get("date"));
            Integer cents = (Integer) day.getOrDefault("sales", 0);
            salesData.add(cents); // 不做換算
        }

        // 訂單狀態分佈（本月）
        Map<String, Long> statusStats = statisticsService.getOrderStatusStatistics(startOfMonth, endOfMonth);
        List<String> statusLabels = new ArrayList<>(statusStats.keySet());
        List<Long> statusData = new ArrayList<>();
        for (String k : statusLabels) {
            statusData.add(statusStats.getOrDefault(k, 0L));
        }

        model.addAttribute("monthlyStats", monthlyStats);
        model.addAttribute("monthlySalesYuan", monthlySalesYuan);
        model.addAttribute("userStats", userStats);
        model.addAttribute("bookCount", bookCount);
        model.addAttribute("salesLabels", salesLabels);
        model.addAttribute("salesData", salesData);
        model.addAttribute("orderStatusLabels", statusLabels);
        model.addAttribute("orderStatusData", statusData);
        model.addAttribute("activeMenu", "dashboard");

        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String userManagement(Model model,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        Boolean statusBool = null;
        if (status != null && !status.isBlank()) {
            if ("true".equalsIgnoreCase(status)) statusBool = Boolean.TRUE;
            else if ("false".equalsIgnoreCase(status)) statusBool = Boolean.FALSE;
        }
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var pageData = userService.searchUsers(keyword, statusBool, pageable);
        model.addAttribute("pageData", pageData);
        model.addAttribute("users", pageData.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        // 會員等級佔位：後續可由外部 API 取得 Map<userId, tier>
        model.addAttribute("membershipTiers", java.util.Collections.emptyMap());
        model.addAttribute("activeMenu", "users");
        return "admin/users";
    }
    
    // 取得目前登入管理員的 ID（由 email 反查）
    private Long getCurrentAdminId(Authentication authentication) {
        String email = authentication.getName();
        return userService.getUserByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new RuntimeException("找不到管理員帳號: " + email));
    }

    @PostMapping("/users/{id}/toggle-status")
    @ResponseBody
    public String toggleUserStatus(@PathVariable Long id, @RequestParam Boolean enabled, Authentication authentication) {
        try {
            User user = userService.updateUserStatus(id, enabled);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                enabled ? "ENABLE_USER" : "DISABLE_USER",
                id,
                "USER",
                "使用者狀態變更為: " + (enabled ? "啟用" : "停權")
            );
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/books")
    public String bookManagement(Model model,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        Book.BookStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Book.BookStatus.valueOf(status); } catch (Exception ignored) {}
        }
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("updatedAt").descending());
        var pageData = bookService.searchBooks(keyword, statusEnum, pageable);
        model.addAttribute("pageData", pageData);
        model.addAttribute("books", pageData.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("activeMenu", "books");
        return "admin/books";
    }
    
    @GetMapping("/books/new")
    public String newBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("activeMenu", "books");
        return "admin/book-form";
    }
    
    @PostMapping("/books")
    public String createBook(@ModelAttribute Book book, @RequestParam(value = "coverImage", required = false) MultipartFile coverImage, Authentication authentication) {
        try {
            // 上傳封面
            if (coverImage != null && !coverImage.isEmpty()) {
                String url = bookService.uploadCoverImage(coverImage);
                book.setCoverImageUrl(url);
            }
            Book savedBook = bookService.createBook(book);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "CREATE_BOOK",
                savedBook.getId(),
                "BOOK",
                "新增書籍: " + savedBook.getTitle()
            );
            return "redirect:/admin/books";
        } catch (Exception e) {
            return "redirect:/admin/books?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/books/{id}/edit")
    public String editBookForm(@PathVariable Long id, Model model) {
        Book book = bookService.getBookById(id).orElseThrow(() -> new RuntimeException("書籍不存在"));
        model.addAttribute("book", book);
        model.addAttribute("activeMenu", "books");
        return "admin/book-form";
    }
    
    @PostMapping("/books/{id}")
    public String updateBook(@PathVariable Long id, @ModelAttribute Book book, @RequestParam(value = "coverImage", required = false) MultipartFile coverImage, Authentication authentication) {
        try {
            // 若有新封面，先上傳並寫入 URL
            if (coverImage != null && !coverImage.isEmpty()) {
                String url = bookService.uploadCoverImage(coverImage);
                book.setCoverImageUrl(url);
            }
            Book updatedBook = bookService.updateBook(id, book);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "UPDATE_BOOK",
                id,
                "BOOK",
                "更新書籍: " + updatedBook.getTitle()
            );
            return "redirect:/admin/books";
        } catch (Exception e) {
            return "redirect:/admin/books?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/books/{id}/delete")
    @ResponseBody
    public String deleteBook(@PathVariable Long id, Authentication authentication) {
        try {
            Book book = bookService.getBookById(id).orElseThrow(() -> new RuntimeException("書籍不存在"));
            bookService.deleteBook(id);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "DELETE_BOOK",
                id,
                "BOOK",
                "刪除書籍: " + book.getTitle()
            );
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/orders")
    public String orderManagement(Model model,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        Order.OrderStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Order.OrderStatus.valueOf(status); } catch (Exception ignored) {}
        }
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var pageData = orderService.searchOrders(keyword, statusEnum, pageable);
        model.addAttribute("pageData", pageData);
        model.addAttribute("orders", pageData.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        // 統計：以目前篩選（keyword+status）之全量列表計算
        List<Order> filtered = orderService.searchOrders(keyword);
        final Order.OrderStatus statusFilter = statusEnum; // ensure effectively final for lambda
        if (statusFilter != null) {
            filtered = filtered.stream().filter(o -> o.getStatus() == statusFilter).toList();
        }
        int totalSales = filtered.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PAID || o.getStatus() == Order.OrderStatus.FULFILLED)
                .map(o -> o.getPayableAmount() == null ? 0 : o.getPayableAmount())
                .mapToInt(Integer::intValue)
                .sum();
        long completedCount = filtered.stream().filter(o -> o.getStatus() == Order.OrderStatus.FULFILLED).count();
        long processingCount = filtered.stream().filter(o -> o.getStatus() == Order.OrderStatus.CREATED || o.getStatus() == Order.OrderStatus.PAYING).count();
        model.addAttribute("orderTotalCount", filtered.size());
        model.addAttribute("orderTotalSales", totalSales);
        model.addAttribute("orderCompletedCount", completedCount);
        model.addAttribute("orderProcessingCount", processingCount);

        model.addAttribute("activeMenu", "orders");
        return "admin/orders";
    }
    
    @PostMapping("/orders/{id}/update-status")
    @ResponseBody
    public String updateOrderStatus(@PathVariable Long id, @RequestParam String status, Authentication authentication) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status);
            Order order = orderService.updateOrderStatus(id, orderStatus);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "UPDATE_ORDER_STATUS",
                id,
                "ORDER",
                "訂單狀態變更為: " + orderStatus.name()
            );
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/points")
    public String pointManagement(Model model,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        PointRule currentRule = pointService.getCurrentPointRule();
        model.addAttribute("pointRule", currentRule);

        // 統計卡片：真實數據
        // 1) 總發放點數（僅計正向：消費回饋、後台加點）
        int totalIssued = 0;
        try {
            var reasons = java.util.Arrays.asList(
                com.example.bookstore.entity.PointsLedger.Reason.PURCHASE_REWARD,
                com.example.bookstore.entity.PointsLedger.Reason.ADJUSTMENT
            );
            Integer sumIssued = pointsLedgerRepository.sumIssuedPoints(reasons);
            totalIssued = sumIssued == null ? 0 : sumIssued;
        } catch (Exception ignored) {}
        model.addAttribute("totalIssuedPoints", totalIssued);

        // 2) 有點數會員數（餘額 > 0）
        long memberWithPts = 0L;
        try { memberWithPts = pointsAccountRepository.countByBalanceGreaterThan(0); } catch (Exception ignored) {}
        model.addAttribute("membersWithPoints", memberWithPts);

        // 3) 30 天內即將到期點數（全站）
        int expiringSoon = 0;
        try {
            LocalDateTime from = LocalDateTime.now();
            LocalDateTime to = from.plusDays(30);
            Integer sumExp = pointLotsRepository.sumRemainingPointsExpiringBetweenAll(from, to);
            expiringSoon = sumExp == null ? 0 : sumExp;
        } catch (Exception ignored) {}
        model.addAttribute("expiringSoonPoints", expiringSoon);

        // 原有列表與分頁
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        var userPage = userService.searchUsers(keyword, null, pageable); // 僅 ROLE_USER
        var content = userPage.getContent().stream().map(u -> {
            var acc = pointsAccountRepository.findByUserId(u.getUserId()).orElse(null);
            int balance = acc != null ? acc.getBalance() : 0;
            Integer r = pointReservationRepository.sumActiveReservedByUser(u.getUserId());
            int reserved = r == null ? 0 : r;
            int available = balance - reserved;
            return new UserPointVM(u.getUserId(), u.getEmail(), u.getUsername(), balance, reserved, available);
        }).toList();
        var vmPage = new org.springframework.data.domain.PageImpl<>(content, pageable, userPage.getTotalElements());

        model.addAttribute("pageData", vmPage);
        model.addAttribute("userPoints", content);
        model.addAttribute("keyword", keyword);

        model.addAttribute("activeMenu", "points");
        return "admin/points";
    }
    
    @PostMapping("/points/users/{id}/adjust")
    @ResponseBody
    public String adjustUserPoints(@PathVariable("id") Long userId,
                                   @RequestParam("delta") Integer delta,
                                   @RequestParam(value = "note", required = false) String note,
                                   @RequestParam(value = "overrideExpiryDays", required = false) Integer overrideExpiryDays,
                                   Authentication authentication) {
        try {
            if (delta == null) return "error: delta 必填";
            // 傳遞 overrideExpiryDays（僅對加點有效，服務會自行判斷）
            pointService.adjustPoints(userId, delta, note, overrideExpiryDays);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "ADJUST_POINTS",
                userId,
                "USER",
                "調整點數: " + delta + (note != null ? (" (" + note + ")") : "") + (overrideExpiryDays != null ? (", 到期天數=" + overrideExpiryDays) : "")
            );
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/coupons")
    public String couponManagement(Model model,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        Coupon.CouponStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Coupon.CouponStatus.valueOf(status); } catch (Exception ignored) {}
        }
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("updatedAt").descending());
        var pageData = couponService.searchCoupons(keyword, statusEnum, pageable);
        model.addAttribute("pageData", pageData);
        model.addAttribute("coupons", pageData.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        // 統計（依目前篩選，不限於當前頁）
        List<Coupon> filtered = couponService.searchCoupons(keyword);
        final Coupon.CouponStatus statusFilter = statusEnum; // ensure effectively final for lambda
        if (statusFilter != null) {
            filtered = filtered.stream().filter(c -> c.getStatus() == statusFilter).toList();
        }
        int couponTotal = filtered.size();
        long couponActive = filtered.stream().filter(c -> c.getStatus() == Coupon.CouponStatus.ACTIVE).count();
        long couponPaused = filtered.stream().filter(c -> c.getStatus() == Coupon.CouponStatus.PAUSED).count();
        long couponExpired = filtered.stream().filter(c -> c.getStatus() == Coupon.CouponStatus.EXPIRED).count();
        model.addAttribute("couponTotal", couponTotal);
        model.addAttribute("couponActive", couponActive);
        model.addAttribute("couponPaused", couponPaused);
        model.addAttribute("couponExpired", couponExpired);

        model.addAttribute("activeMenu", "coupons");
        return "admin/coupons";
    }
    
    @GetMapping("/coupons/new")
    public String newCouponForm(Model model) {
        model.addAttribute("coupon", new Coupon());
        model.addAttribute("activeMenu", "coupons");
        return "admin/coupon-form";
    }
    
    @PostMapping("/coupons")
    public String createCoupon(@ModelAttribute Coupon coupon, Authentication authentication) {
        try {
            Coupon savedCoupon = couponService.createCoupon(coupon);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "CREATE_COUPON",
                savedCoupon.getId(),
                "COUPON",
                "新增優惠券: " + savedCoupon.getName()
            );
            return "redirect:/admin/coupons";
        } catch (Exception e) {
            return "redirect:/admin/coupons?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/coupons/{id}/edit")
    public String editCouponForm(@PathVariable Long id, Model model) {
        Coupon coupon = couponService.getCouponById(id).orElseThrow(() -> new RuntimeException("優惠券不存在"));
        model.addAttribute("coupon", coupon);
        model.addAttribute("activeMenu", "coupons");
        return "admin/coupon-form";
    }
    
    @PostMapping("/coupons/{id}")
    public String updateCoupon(@PathVariable Long id, @ModelAttribute Coupon coupon, Authentication authentication) {
        try {
            Coupon updatedCoupon = couponService.updateCoupon(id, coupon);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "UPDATE_COUPON",
                id,
                "COUPON",
                "更新優惠券: " + updatedCoupon.getName()
            );
            return "redirect:/admin/coupons";
        } catch (Exception e) {
            return "redirect:/admin/coupons?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/coupons/{id}/delete")
    @ResponseBody
    public String deleteCoupon(@PathVariable Long id, Authentication authentication) {
        try {
            Coupon coupon = couponService.getCouponById(id).orElseThrow(() -> new RuntimeException("優惠券不存在"));
            couponService.deleteCoupon(id);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "DELETE_COUPON",
                id,
                "COUPON",
                "刪除優惠券: " + coupon.getName()
            );
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/logs")
    public String adminLogs(Model model,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String action,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        // 正規化參數
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        var pageData = adminLogService.searchLogs(keyword, action, pageable);
        model.addAttribute("pageData", pageData);
        model.addAttribute("logs", pageData.getContent()); // 若模板有用到
        model.addAttribute("keyword", keyword);
        model.addAttribute("action", action);

        // 統計數據（依當前篩選條件）
        long logTotal = adminLogService.countLogs(keyword, action);
        long logCreate = adminLogService.countCreate(keyword, action);
        long logUpdate = adminLogService.countUpdate(keyword, action);
        long logDelete = adminLogService.countDelete(keyword, action);
        model.addAttribute("logTotal", logTotal);
        model.addAttribute("logCreate", logCreate);
        model.addAttribute("logUpdate", logUpdate);
        model.addAttribute("logDelete", logDelete);

        // 操作趨勢（最近 6 個月）
        model.addAttribute("trend", adminLogService.getMonthlyTrend(6));

        model.addAttribute("activeMenu", "logs");
        return "admin/logs";
    }


    @PostMapping("/points/update-rules")
    public String updatePointRules(@ModelAttribute("pointRule") PointRule form, Authentication authentication) {
        try {
            // 正規化輸入
            if (form.getExpiryPolicy() == PointRule.ExpiryPolicy.NONE) {
                form.setRollingDays(null);
                form.setFixedExpireDay(null);
            } else if (form.getExpiryPolicy() == PointRule.ExpiryPolicy.ROLLING_DAYS) {
                form.setFixedExpireDay(null);
            } else if (form.getExpiryPolicy() == PointRule.ExpiryPolicy.FIXED_DATE) {
                form.setRollingDays(null);
            }
            pointService.updatePointRule(form);
            adminLogService.logAction(
                getCurrentAdminId(authentication),
                "UPDATE_POINT_RULE",
                null,
                "POINT_RULE",
                "更新點數規則"
            );
            return "redirect:/admin/points";
        } catch (Exception e) {
            return "redirect:/admin/points?error=" + e.getMessage();
        }
    }

    // 供點數頁面使用的簡單 ViewModel
    private static class UserPointVM {
        private final Long userId;
        private final String email;
        private final String username;
        private final int balance;
        private final int reserved;
        private final int available;
        private UserPointVM(Long userId, String email, String username, int balance, int reserved, int available) {
            this.userId = userId;
            this.email = email;
            this.username = username;
            this.balance = balance;
            this.reserved = reserved;
            this.available = available;
        }
        public Long getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getUsername() { return username; }
        public int getBalance() { return balance; }
        public int getReserved() { return reserved; }
        public int getAvailable() { return available; }
    }
}
