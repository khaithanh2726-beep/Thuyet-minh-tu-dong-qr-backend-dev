package com.qraudio.backend.controller;

import com.qraudio.backend.dto.*;
import com.qraudio.backend.service.AdminQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/qrcodes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - QR")
public class AdminQrController {
    private final AdminQrService service;
    public AdminQrController(AdminQrService service) { this.service = service; }
    @GetMapping public ApiResponse<List<QrResponse>> list() { return ApiResponse.success(service.list()); }
    @GetMapping("/{id}") public ApiResponse<QrResponse> get(@PathVariable Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ResponseEntity<ApiResponse<QrResponse>> create(@Valid @RequestBody CreateQrRequest request) {
        return ResponseEntity.status(201).body(new ApiResponse<>(201, "Đã tạo QR", service.create(request)));
    }
    @PatchMapping("/{id}") public ApiResponse<QrResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateQrRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id); return ResponseEntity.noContent().build();
    }
    @GetMapping(value = "/{id}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Tải ảnh QR của hiện vật theo ngôn ngữ", description = "Truyền lang để QR mở MP3 trực tiếp; QR và hiện vật phải đang hoạt động, có bản dịch tương ứng. Bỏ lang để lấy QR metadata.")
    public byte[] image(@PathVariable Long id,
                        @Parameter(example = "vi-vn") @RequestParam(required = false) String lang) {
        return service.adminImage(id, lang);
    }
}
