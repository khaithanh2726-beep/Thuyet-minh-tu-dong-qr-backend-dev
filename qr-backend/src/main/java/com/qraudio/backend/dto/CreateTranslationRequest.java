package com.qraudio.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTranslationRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z]{2}-[a-zA-Z]{2}") String languageCode,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 65535) String content) {
}
