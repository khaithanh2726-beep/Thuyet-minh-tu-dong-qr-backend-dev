package com.qraudio.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_codes")
public class QrCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // Id QR
    private Long exhibitId;          // Id hiện vật
    @Column(nullable = false, unique = true, length = 80)
    private String code;             // Chuỗi mã QR
    private boolean isActive;        // Trạng thái (true: đang hoạt động, false: vô hiệu hóa)
    private LocalDateTime createdAt; // Ngày tạo mã

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExhibitId() { return exhibitId; }
    public void setExhibitId(Long exhibitId) { this.exhibitId = exhibitId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
