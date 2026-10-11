package com.qraudio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CreateExhibitRequest(
        @Schema(description = "Đường dẫn ảnh; có thể bỏ qua nếu chưa có ảnh")
        @Size(max = 1000) String imageUrl,
        @Schema(description = "Trạng thái trưng bày; mặc định true khi bỏ qua",
                example = "true", defaultValue = "true") Boolean active) {
}
