package com.qraudio.backend.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
public record QrResponse(Long id, Long exhibitId, String code, boolean active,
        LocalDateTime createdAt,
        @Schema(description = "URL API metadata được mã hóa trong PNG; trả thông tin hiện vật và các bản dịch có sẵn") String qrUrl,
        @Schema(description = "Đường dẫn API tải PNG, yêu cầu JWT Admin") String imageUrl) {}
