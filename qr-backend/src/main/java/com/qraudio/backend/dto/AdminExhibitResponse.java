package com.qraudio.backend.dto;

import com.qraudio.backend.entity.Exhibit;

public record AdminExhibitResponse(Long id, String imageUrl, boolean active) {
    public static AdminExhibitResponse from(Exhibit exhibit) {
        return new AdminExhibitResponse(exhibit.getId(), exhibit.getImageUrl(), exhibit.isActive());
    }
}
