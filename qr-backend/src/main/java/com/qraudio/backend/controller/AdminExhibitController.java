package com.qraudio.backend.controller;

import com.qraudio.backend.dto.AdminExhibitResponse;
import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.CreateExhibitRequest;
import com.qraudio.backend.dto.CreateTranslationRequest;
import com.qraudio.backend.dto.TranslationResponse;
import com.qraudio.backend.dto.UpdateExhibitRequest;
import com.qraudio.backend.dto.UpdateTranslationRequest;
import com.qraudio.backend.service.AdminExhibitService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/exhibits")
@SecurityRequirement(name = "bearerAuth")
public class AdminExhibitController {
    private final AdminExhibitService service;

    public AdminExhibitController(AdminExhibitService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<AdminExhibitResponse>> list() { return ApiResponse.success(service.list()); }

    @GetMapping("/{id}")
    public ApiResponse<AdminExhibitResponse> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminExhibitResponse>> create(
            @Valid @RequestBody CreateExhibitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Tạo hiện vật thành công", service.create(request)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminExhibitResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateExhibitRequest request) {
        return ApiResponse.success("Cập nhật hiện vật thành công", service.update(id, request));
    }

    // DELETE hides the exhibit and preserves its translations and QR codes.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/translations")
    public ApiResponse<List<TranslationResponse>> listTranslations(@PathVariable Long id) {
        return ApiResponse.success(service.listTranslations(id));
    }

    @GetMapping("/{id}/translations/{languageCode}")
    public ApiResponse<TranslationResponse> getTranslation(@PathVariable Long id,
                                                           @PathVariable String languageCode) {
        return ApiResponse.success(service.getTranslation(id, languageCode));
    }

    @PostMapping("/{id}/translations")
    public ResponseEntity<ApiResponse<TranslationResponse>> createTranslation(@PathVariable Long id,
            @Valid @RequestBody CreateTranslationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201,
                "Tạo bản dịch thành công", service.createTranslation(id, request)));
    }

    @PutMapping("/{id}/translations/{languageCode}")
    public ApiResponse<TranslationResponse> updateTranslation(@PathVariable Long id,
            @PathVariable String languageCode, @Valid @RequestBody UpdateTranslationRequest request) {
        return ApiResponse.success("Cập nhật bản dịch thành công",
                service.updateTranslation(id, languageCode, request));
    }

    @DeleteMapping("/{id}/translations/{languageCode}")
    public ResponseEntity<Void> deleteTranslation(@PathVariable Long id,
                                                  @PathVariable String languageCode) {
        service.deleteTranslation(id, languageCode);
        return ResponseEntity.noContent().build();
    }
}
