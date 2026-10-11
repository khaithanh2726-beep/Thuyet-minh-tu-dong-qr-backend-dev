package com.qraudio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTranslationRequest(
        @Schema(description = "Mã ngôn ngữ trong GET /api/languages", example = "vi-vn")
        @NotBlank @Pattern(regexp = "[a-zA-Z]{2}-[a-zA-Z]{2}") String languageCode,
        @Schema(description = "Tên hiện vật theo ngôn ngữ của bản dịch")
        @NotBlank @Size(max = 255) String title,
        @Schema(description = "Nội dung thuyết minh dùng để tạo audio")
        @NotBlank @Size(max = 65535) String content) {
}
