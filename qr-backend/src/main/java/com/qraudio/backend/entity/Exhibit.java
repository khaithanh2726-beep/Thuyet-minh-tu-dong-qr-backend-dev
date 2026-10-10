package com.qraudio.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exhibits")
public class Exhibit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;            // Id hiện vật
    @Column(length = 1000)
    private String imageUrl;    // Link ảnh hiện vật
    @Column(nullable = false)
    private boolean isActive;   // Trạng thái hiện vật (true: đang trưng bày, false: đang cất)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
