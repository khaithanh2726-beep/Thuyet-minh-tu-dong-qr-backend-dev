package com.qraudio.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exhibit_translations", uniqueConstraints =
        @UniqueConstraint(name = "uq_translation_exhibit_language",
                columnNames = {"exhibit_id", "language_code"}))
public class ExhibitTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // Id bản dịch hiện vật
    @Column(name = "exhibit_id", nullable = false)
    private Long exhibitId;         // Id hiện vật
    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;    // Ngôn ngữ bản dịch
    @Column(nullable = false, length = 255)
    private String title;           // Tên hiện vật
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;         // Nội dung bản dịch

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExhibitId() { return exhibitId; }
    public void setExhibitId(Long exhibitId) { this.exhibitId = exhibitId; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
