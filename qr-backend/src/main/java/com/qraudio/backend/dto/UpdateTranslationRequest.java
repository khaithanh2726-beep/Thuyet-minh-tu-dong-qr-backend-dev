package com.qraudio.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTranslationRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 65535) String content) {
}
