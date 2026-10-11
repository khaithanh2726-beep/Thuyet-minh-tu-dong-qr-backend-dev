package com.qraudio.backend.controller;

import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.dto.QrInfoResponse;
import com.qraudio.backend.service.AdminQrService;
import com.qraudio.backend.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
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

    @Operation(summary = "Tải QR theo ngôn ngữ để nghe MP3", description = "Chọn code từ GET /api/catalog và lang từ languages của hiện vật, ví dụ vi-vn hoặc en-us. QR chứa URL phát MP3 của ngôn ngữ đã chọn; tạo ảnh không gọi VoiceRSS. Bỏ lang để lấy QR metadata như trước. Cần PUBLIC_BASE_URL là địa chỉ điện thoại truy cập được.")
    @GetMapping(value = "/{code}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] image(@PathVariable String code,
                        @Parameter(description = "Ngôn ngữ có bản dịch của hiện vật; bỏ trống để lấy QR metadata", example = "vi-vn")
                        @RequestParam(required = false) String lang) { return adminQrService.publicImage(code, lang); }

    @Operation(summary = "Nghe MP3 theo mã QR và ngôn ngữ", description = "URL được mã hóa trong QR theo ngôn ngữ. Trả MP3 trực tiếp để trình duyệt điện thoại mở trình phát; có thể cần bấm Play. Swagger cũng hiện thanh phát sau khi Execute. Audio được tạo lần đầu và dùng lại từ cache.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audio MP3", content = @Content(mediaType = "audio/mpeg", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/{code}/audio")
    public ResponseEntity<Resource> audio(@PathVariable String code,
                                          @Parameter(description = "Ngôn ngữ đã chọn khi tạo QR", example = "vi-vn")
                                          @RequestParam String lang) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.noStore())
                .body(qrCodeService.getAudio(code, lang));
    }

    @Operation(summary = "Lấy bản dịch và audio theo mã QR/ngôn ngữ", description = "Trả JSON chứa title, content, languageCode và audioUrl. Gọi GET audioUrl trên cùng backend để tải MP3; backend tái sử dụng audio đã tạo.")
    @GetMapping("/resolve/{code}")
    public ApiResponse<ExhibitResponse> resolveQr(
            @PathVariable String code,
            @RequestParam(defaultValue = "vi-vn") String lang) {
        
        return ApiResponse.success(qrCodeService.resolveQrCode(code, lang));
    }
}
