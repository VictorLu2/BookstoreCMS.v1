package com.example.bookstore.service;

import com.example.bookstore.entity.Role;
import com.example.bookstore.entity.User;
import com.example.bookstore.repository.RoleRepository;
import com.example.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public List<User> getAllUsers() {
        return userRepository.findAllUsers();
    }
    
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers();
        }
        return userRepository.findByKeyword(keyword.trim());
    }
    
    public List<User> getUsersByStatus(Boolean enabled) {
        return userRepository.findByEnabled(enabled);
    }
    
    public User updateUserStatus(Long userId, Boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("使用者不存在"));
        user.setEnabled(enabled);
        return userRepository.save(user);
    }
    
    public User updateUser(Long userId, User userDetails) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("使用者不存在"));
        
        if (userDetails.getUsername() != null) {
            user.setUsername(userDetails.getUsername());
        }
        if (userDetails.getGender() != null) {
            user.setGender(userDetails.getGender());
        }
        if (userDetails.getBirthdate() != null) {
            user.setBirthdate(userDetails.getBirthdate());
        }

        // 可選：更新密碼（如有帶入且非空）
        if (userDetails.getPasswordHash() != null && !userDetails.getPasswordHash().isBlank()) {
            String rawOrHashed = userDetails.getPasswordHash();
            if (!(rawOrHashed.startsWith("$2a$") || rawOrHashed.startsWith("$2b$") || rawOrHashed.startsWith("$2y$"))) {
                user.setPasswordHash(passwordEncoder.encode(rawOrHashed));
            } else {
                user.setPasswordHash(rawOrHashed);
            }
        }

        return userRepository.save(user);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User createUser(User user) {
        if (existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email 已存在");
        }
        // 預設角色
        if (user.getRole() == null) {
            Role role = roleRepository.findByRoleName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("找不到預設角色 ROLE_USER"));
            user.setRole(role);
        }
        // 預設啟用
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        // 處理密碼編碼
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            String rawOrHashed = user.getPasswordHash();
            if (!(rawOrHashed.startsWith("$2a$") || rawOrHashed.startsWith("$2b$") || rawOrHashed.startsWith("$2y$"))) {
                user.setPasswordHash(passwordEncoder.encode(rawOrHashed));
            }
        } else {
            throw new IllegalArgumentException("密碼不可為空");
        }
        return userRepository.save(user);
    }

    public Page<User> searchUsers(String keyword, Boolean status, Pageable pageable) {
        return userRepository.searchUsersForAdmin(keyword, status, pageable);
    }
}
