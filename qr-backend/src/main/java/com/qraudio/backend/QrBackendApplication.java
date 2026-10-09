package com.qraudio.backend;

import com.qraudio.backend.entity.User;
import com.qraudio.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@SpringBootApplication
public class QrBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Kiểm tra tài khoản Admin
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                
                // Tạo mật khẩu - 123456
                admin.setPasswordHash(passwordEncoder.encode("123456")); 
                admin.setRole("ROLE_ADMIN");
                admin.setActive(true);
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);
            }
        };
    }
}