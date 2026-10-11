package com.qraudio.backend.controller;

import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.dto.QrInfoResponse;
import com.qraudio.backend.service.AdminQrService;
import com.qraudio.backend.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qrcodes")
@Tag(name = "Public - QR")
public class QrCodeController {
    private final QrCodeService qrCodeService;
    private final AdminQrService adminQrService;

    public QrCodeController(QrCodeService qrCodeService, AdminQrService adminQrService) {
        this.qrCodeService = qrCodeService;
        this.adminQrService = adminQrService;
    }

    @Operation(summary = "Tra cứu hiện vật và các bản dịch từ QR; không tạo audio")
    @GetMapping("/{code}")
    public ApiResponse<QrInfoResponse> info(@PathVariable String code) { return ApiResponse.success(qrCodeService.getInfo(code)); }

    @Operation(summary = "Tải ảnh QR PNG trỏ tới API metadata")
    @GetMapping(value = "/{code}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] image(@PathVariable String code) { return adminQrService.publicImage(code); }

    @Operation(summary = "Lấy bản dịch và audio theo mã QR/ngôn ngữ", description = "Trả JSON chứa title, content, languageCode và audioUrl. Gọi GET audioUrl trên cùng backend để tải MP3; backend tái sử dụng audio đã tạo.")
    @GetMapping("/resolve/{code}")
    public ApiResponse<ExhibitResponse> resolveQr(
            @PathVariable String code,
            @RequestParam(defaultValue = "vi-vn") String lang) {
        
        return ApiResponse.success(qrCodeService.resolveQrCode(code, lang));
    }
}
