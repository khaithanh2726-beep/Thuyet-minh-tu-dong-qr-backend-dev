package com.qraudio.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exhibit_translations")
public class ExhibitTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // Id bản dịch hiện vật
    private Long exhibitId;         // Id hiện vật
    private String languageCode;    // Ngôn ngữ bản dịch
    private String title;           // Tên hiện vật
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