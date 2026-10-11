package com.qraudio.backend.dto;

import java.util.List;

public record ExhibitInfoResponse(Long id, String imageUrl, List<Translation> languages, String qrCode) {
    public record Translation(String code, String name, String title, String content) {}
}
