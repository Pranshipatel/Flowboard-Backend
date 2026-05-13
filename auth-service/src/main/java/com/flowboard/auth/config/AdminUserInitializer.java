package com.flowboard.auth.config;

import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    @Value("${FLOWBOARD_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${FLOWBOARD_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${FLOWBOARD_ADMIN_FULL_NAME:Platform Admin}")
    private String adminFullName;

    @Value("${FLOWBOARD_ADMIN_USERNAME:platform_admin}")
    private String adminUsername;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("Skipping platform admin seed because FLOWBOARD_ADMIN_EMAIL or FLOWBOARD_ADMIN_PASSWORD is not set.");
            return;
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .fullName(adminFullName)
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(ROLE.PLATFORM_ADMIN)
                    .emailVerified(true)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
            userRepository.save(admin);
            log.info("Admin user {} created successfully.", adminEmail);
        } else {
            User admin = userRepository.findByEmail(adminEmail).orElseThrow();
            boolean changed = ensureAdminState(admin);
            
            if (changed) {
                userRepository.save(admin);
                log.info("Admin user {} updated successfully to PLATFORM_ADMIN.", adminEmail);
            }
        }
    }

    private boolean ensureAdminState(User admin) {
        boolean changed = false;
        if (admin.getRole() != ROLE.PLATFORM_ADMIN) {
            admin.setRole(ROLE.PLATFORM_ADMIN);
            changed = true;
        }
        if (!admin.isEmailVerified()) {
            admin.setEmailVerified(true);
            changed = true;
        }
        if (!admin.isActive()) {
            admin.setActive(true);
            changed = true;
        }
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
        }
        return changed;
    }
}
