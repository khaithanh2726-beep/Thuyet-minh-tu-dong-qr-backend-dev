package com.qraudio.backend.config;

import com.qraudio.backend.entity.User;
import com.qraudio.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("!test")
public class BootstrapAdminConfig {
    @Bean
    CommandLineRunner bootstrapAdmin(UserRepository users, PasswordEncoder encoder,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:admin}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        return args -> {
            // Opt-in bootstrap for an empty database; never reset existing accounts.
            if (users.count() != 0 || password.isBlank()) { return; }
            if (!username.matches("[A-Za-z0-9._-]{1,60}") || password.length() < 8
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Cấu hình tài khoản Admin ban đầu không hợp lệ");
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setPasswordHash(encoder.encode(password));
            admin.setRole("ADMIN");
            admin.setActive(true);
            users.saveAndFlush(admin);
        };
    }
}
