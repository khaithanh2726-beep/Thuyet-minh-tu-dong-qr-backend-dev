package com.qraudio.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // Id tài khoản
    @Column(nullable = false, unique = true, length = 60)
    private String username;         // Tên đăng nhập
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String passwordHash;     // Mật khẩu
    @Column(name = "`role`", nullable = false, length = 30)
    private String role;             // Quyền hạn
    private boolean isActive;        // Trạng thái (true: đang hoạt động, false: bị khóa)
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt; // Ngày tạo tài khoản
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private long tokenVersion;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(Clock.systemUTC());
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(Clock.systemUTC()); }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getTokenVersion() { return tokenVersion; }
    public void invalidateTokens() { tokenVersion++; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
