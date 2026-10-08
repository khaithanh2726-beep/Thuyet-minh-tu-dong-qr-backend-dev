package com.qraudio.backend.controller;

import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.service.QrCodeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qrcodes")
public class QrCodeController {
    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    // GET: /api/qrcodes/resolve/{code}
    @GetMapping("/resolve/{code}")
    public ApiResponse<ExhibitResponse> resolveQr(
            @PathVariable String code,
            @RequestParam(defaultValue = "vi-vn") String lang) {
        
        ExhibitResponse data = qrCodeService.resolveQrCode(code, lang);
        return ApiResponse.success(data);
    }
}