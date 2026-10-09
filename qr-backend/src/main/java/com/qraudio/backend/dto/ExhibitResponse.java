package com.qraudio.backend.dto;

public class ExhibitResponse {
    private Long id;              // Id của hiện vật
    private String imageUrl;      // Link ảnh hiện vật
    private String languageCode;  // Ngôn ngữ bản dịch
    private String title;         // Tên hiện vật 
    private String content;       // Nội dung bản dịch
    private String audioUrl;      // Link file bản dịch

    public ExhibitResponse(Long id, String imageUrl, String languageCode, String title, String content, String audioUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.languageCode = languageCode;
        this.title = title;
        this.content = content;
        this.audioUrl = audioUrl;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getLanguageCode() { return languageCode; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAudioUrl() { return audioUrl; }
}