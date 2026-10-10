package com.qraudio.backend.dto;

import com.qraudio.backend.entity.ExhibitTranslation;

public record TranslationResponse(Long id, Long exhibitId, String languageCode,
                                  String title, String content) {
    public static TranslationResponse from(ExhibitTranslation translation) {
        return new TranslationResponse(translation.getId(), translation.getExhibitId(),
                translation.getLanguageCode(), translation.getTitle(), translation.getContent());
    }
}
