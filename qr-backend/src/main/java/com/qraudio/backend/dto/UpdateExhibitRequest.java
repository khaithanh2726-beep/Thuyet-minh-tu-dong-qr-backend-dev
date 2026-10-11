package com.qraudio.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateExhibitRequest(
        @Schema(description = "Đường dẫn ảnh mới; gửi chuỗi rỗng để bỏ đường dẫn ảnh")
        @Size(max = 1000) String imageUrl,
        @Schema(description = "true để trưng bày, false để vô hiệu hóa", example = "true") Boolean active) {
}
