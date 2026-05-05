package com.flowboard.auth.config;

import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "nainshipatel77@gmail.com";
        
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .fullName("Nainshi Patel")
                    .username("nainshi_admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Nainshi@123"))
                    .role(ROLE.PLATFORM_ADMIN)
                    .emailVerified(true)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
            userRepository.save(admin);
            log.info("Admin user {} created successfully.", adminEmail);
        } else {
            User admin = userRepository.findByEmail(adminEmail).orElseThrow();
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
            // Update password just in case it was different
            admin.setPassword(passwordEncoder.encode("Nainshi@123"));
            changed = true;
            
            if (changed) {
                userRepository.save(admin);
                log.info("Admin user {} updated successfully to PLATFORM_ADMIN.", adminEmail);
            }
        }
    }
}
