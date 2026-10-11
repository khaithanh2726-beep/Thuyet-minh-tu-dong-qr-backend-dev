package com.qraudio.backend.dto;

public record ExhibitResponse(Long id, String imageUrl, String languageCode,
                              String title, String content, String audioUrl) {}
