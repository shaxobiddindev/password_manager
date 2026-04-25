package com.company.passwordmanager.config;

import com.company.passwordmanager.entity.User;
import com.company.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.login}")
    private String adminLogin;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByLogin(adminLogin)) {
            User admin = User.builder()
                    .email(adminEmail)
                    .login(adminLogin)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("==============================================");
            log.info("  Superadmin created successfully!");
            log.info("  Login: {}", adminLogin);
            log.info("  Email: {}", adminEmail);
            log.info("==============================================");
        } else {
            log.info("Superadmin already exists, skipping creation.");
        }
    }
}
