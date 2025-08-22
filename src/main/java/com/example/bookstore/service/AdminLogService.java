package com.example.bookstore.service;

import com.example.bookstore.entity.AdminLog;
import com.example.bookstore.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLogService {
    
    private final AdminLogRepository adminLogRepository;
    
    /**
     * 記錄管理員操作
     */
    public AdminLog logAction(Long adminId, String action, Long targetId, String targetType, String details) {
        AdminLog log = new AdminLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setDetails(details);
        
        return adminLogRepository.save(log);
    }
    
    /**
     * 獲取所有日誌
     */
    public List<AdminLog> getAllLogs() {
        return adminLogRepository.findAll();
    }
    
    /**
     * 根據管理員ID獲取日誌
     */
    public List<AdminLog> getLogsByAdminId(Long adminId) {
        return adminLogRepository.findByAdminId(adminId);
    }
    
    /**
     * 根據操作類型獲取日誌
     */
    public List<AdminLog> getLogsByAction(String action) {
        return adminLogRepository.findByAction(action);
    }
    
    /**
     * 根據目標類型獲取日誌
     */
    public List<AdminLog> getLogsByTargetType(String targetType) {
        return adminLogRepository.findByTargetType(targetType);
    }
    
    /**
     * 搜尋日誌
     */
    public List<AdminLog> searchLogs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllLogs();
        }
        return adminLogRepository.findByKeyword(keyword.trim());
    }

    /**
     * 取得最近 N 個月的操作趨勢資料（CREATE/UPDATE/DELETE）
     */
    public Map<String, Object> getMonthlyTrend(int months) {
        if (months <= 0) months = 6;
        YearMonth startYm = YearMonth.now().minusMonths(months - 1);
        LocalDateTime from = startYm.atDay(1).atStartOfDay();

        List<Object[]> rows = adminLogRepository.findMonthlyActionCounts(from);
        Map<YearMonth, int[]> map = new HashMap<>();
        for (Object[] r : rows) {
            int year = ((Number) r[0]).intValue();
            int month = ((Number) r[1]).intValue();
            int cCreate = ((Number) r[2]).intValue();
            int cUpdate = ((Number) r[3]).intValue();
            int cDelete = ((Number) r[4]).intValue();
            map.put(YearMonth.of(year, month), new int[]{cCreate, cUpdate, cDelete});
        }

        List<String> labels = new ArrayList<>();
        List<Integer> creates = new ArrayList<>();
        List<Integer> updates = new ArrayList<>();
        List<Integer> deletes = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            YearMonth ym = startYm.plusMonths(i);
            labels.add(ym.getMonthValue() + "月");
            int[] v = map.getOrDefault(ym, new int[]{0, 0, 0});
            creates.add(v[0]);
            updates.add(v[1]);
            deletes.add(v[2]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("create", creates);
        result.put("update", updates);
        result.put("delete", deletes);
        return result;
    }

    public Page<AdminLog> searchLogs(String keyword, String action, Pageable pageable) {
        return adminLogRepository.search(keyword, action, pageable);
    }

    public long countLogs(String keyword, String action) {
        return adminLogRepository.countByFilters(keyword, action);
    }

    public long countCreate(String keyword, String action) {
        return adminLogRepository.countByTypePrefix(keyword, action, "CREATE");
    }

    public long countUpdate(String keyword, String action) {
        return adminLogRepository.countByTypePrefix(keyword, action, "UPDATE");
    }

    public long countDelete(String keyword, String action) {
        return adminLogRepository.countByTypePrefix(keyword, action, "DELETE");
    }
}
