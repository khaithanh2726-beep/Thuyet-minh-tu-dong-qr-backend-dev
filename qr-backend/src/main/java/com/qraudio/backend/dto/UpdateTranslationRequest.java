package com.qraudio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTranslationRequest(
        @Schema(description = "Tên hiện vật theo ngôn ngữ của bản dịch")
        @NotBlank @Size(max = 255) String title,
        @Schema(description = "Nội dung thuyết minh mới; thay đổi nội dung sẽ tạo lại audio khi được yêu cầu")
        @NotBlank @Size(max = 65535) String content) {
}
