package com.qraudio.backend.controller;

import com.qraudio.backend.dto.AdminExhibitResponse;
import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.CreateExhibitRequest;
import com.qraudio.backend.dto.CreateTranslationRequest;
import com.qraudio.backend.dto.TranslationResponse;
import com.qraudio.backend.dto.UpdateExhibitRequest;
import com.qraudio.backend.dto.UpdateTranslationRequest;
import com.qraudio.backend.service.AdminExhibitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/exhibits")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Hiện vật và bản dịch", description = "Quản lý hiện vật và nội dung đa ngôn ngữ; yêu cầu tài khoản Admin")
public class AdminExhibitController {
    private final AdminExhibitService service;

    public AdminExhibitController(AdminExhibitService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Xem danh sách hiện vật, gồm cả hiện vật đã vô hiệu hóa")
    public ApiResponse<List<AdminExhibitResponse>> list() { return ApiResponse.success(service.list()); }

    @GetMapping("/{id}")
    @Operation(summary = "Xem thông tin quản trị của một hiện vật")
    public ApiResponse<AdminExhibitResponse> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    @Operation(summary = "Tạo hiện vật")
    public ResponseEntity<ApiResponse<AdminExhibitResponse>> create(
            @Valid @RequestBody CreateExhibitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "Tạo hiện vật thành công", service.create(request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật đường dẫn ảnh hoặc trạng thái hiện vật")
    public ApiResponse<AdminExhibitResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateExhibitRequest request) {
        return ApiResponse.success("Cập nhật hiện vật thành công", service.update(id, request));
    }

    // DELETE hides the exhibit and preserves its translations and QR codes.
    @DeleteMapping("/{id}")
    @Operation(summary = "Vô hiệu hóa hiện vật", description = "Giữ lại hiện vật, bản dịch và QR; khách không còn đọc được hiện vật này")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/translations")
    @Operation(summary = "Xem danh sách bản dịch của hiện vật")
    public ApiResponse<List<TranslationResponse>> listTranslations(@PathVariable Long id) {
        return ApiResponse.success(service.listTranslations(id));
    }

    @GetMapping("/{id}/translations/{languageCode}")
    @Operation(summary = "Xem một bản dịch theo mã ngôn ngữ")
    public ApiResponse<TranslationResponse> getTranslation(@PathVariable Long id,
                                                           @PathVariable String languageCode) {
        return ApiResponse.success(service.getTranslation(id, languageCode));
    }

    @PostMapping("/{id}/translations")
    @Operation(summary = "Thêm bản dịch", description = "Mỗi hiện vật chỉ có một bản dịch cho mỗi mã ngôn ngữ")
    public ResponseEntity<ApiResponse<TranslationResponse>> createTranslation(@PathVariable Long id,
            @Valid @RequestBody CreateTranslationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201,
                "Tạo bản dịch thành công", service.createTranslation(id, request)));
    }

    @PutMapping("/{id}/translations/{languageCode}")
    @Operation(summary = "Thay nội dung bản dịch", description = "Gửi đủ title và content; đổi content sẽ bỏ metadata audio cache của bản dịch")
    public ApiResponse<TranslationResponse> updateTranslation(@PathVariable Long id,
            @PathVariable String languageCode, @Valid @RequestBody UpdateTranslationRequest request) {
        return ApiResponse.success("Cập nhật bản dịch thành công",
                service.updateTranslation(id, languageCode, request));
    }

    @DeleteMapping("/{id}/translations/{languageCode}")
    @Operation(summary = "Xóa bản dịch", description = "Xóa bản dịch và metadata audio liên quan; giữ lại các bản dịch ngôn ngữ khác")
    public ResponseEntity<Void> deleteTranslation(@PathVariable Long id,
                                                  @PathVariable String languageCode) {
        service.deleteTranslation(id, languageCode);
        return ResponseEntity.noContent().build();
    }
}
