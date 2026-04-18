package com.smart_desk.demo.config;

import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@smartdesk.dev";
    private static final String ADMIN_PASSWORD = "Admin1234!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) return;

        userRepository.save(User.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .fullName("Admin User")
                .role(User.Role.ADMIN)
                .active(true)
                .build());

        log.info("Seeded default admin: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
