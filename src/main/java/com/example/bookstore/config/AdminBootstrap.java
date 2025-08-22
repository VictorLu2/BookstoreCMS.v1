package com.example.bookstore.config;

import com.example.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 校正 admin 與 user 的密碼，確保可登入
            resetIfMismatch("admin@bookstore.com", "admin123");
            resetIfMismatch("user@bookstore.com", "user123");
        } catch (Exception e) {
            log.error("AdminBootstrap 初始化失敗: {}", e.getMessage(), e);
            // 不再拋出，避免影響應用啟動
        }
    }

    private void resetIfMismatch(String email, String rawPassword) {
        try {
            userRepository.findByEmail(email).ifPresent(u -> {
                String hash = u.getPasswordHash();
                if (hash == null || !passwordEncoder.matches(rawPassword, hash)) {
                    u.setPasswordHash(passwordEncoder.encode(rawPassword));
                    userRepository.save(u);
                    log.info("Reset password hash for {}", email);
                }
            });
        } catch (Exception e) {
            log.warn("重置使用者密碼失敗 ({})：{}", email, e.getMessage());
        }
    }
}
