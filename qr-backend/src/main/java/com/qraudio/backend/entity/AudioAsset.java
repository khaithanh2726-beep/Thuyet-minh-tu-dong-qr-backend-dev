package com.qraudio.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audio_assets")
public class AudioAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long translationId;  // Id bản dịch hiện vật
    private String provider;     // Nguồn tạo âm thanh (VoiceRSS)
    private String voiceCode;    // Mã ngôn ngữ (vi-vn)
    private String contentHash;  // Mã hash của đoạn text
    private String format;       // Định dạng file (mp3, wav)
    private String storageKey;   // Tên/Link file
    
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public AudioAsset() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTranslationId() { return translationId; }
    public void setTranslationId(Long translationId) { this.translationId = translationId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getVoiceCode() { return voiceCode; }
    public void setVoiceCode(String voiceCode) { this.voiceCode = voiceCode; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}