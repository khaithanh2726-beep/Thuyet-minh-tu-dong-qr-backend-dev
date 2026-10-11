package com.qraudio.backend.controller;

import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.dto.ExhibitInfoResponse;
import com.qraudio.backend.service.ExhibitService;
import com.qraudio.backend.service.Languages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Public - Hiện vật")
public class ExhibitController {
    private final ExhibitService exhibitService;

    public ExhibitController(ExhibitService exhibitService) {
        this.exhibitService = exhibitService;
    }

    @Operation(summary = "Danh sách hiện vật và bản dịch đang được trưng bày")
    @GetMapping("/catalog")
    public ApiResponse<List<ExhibitInfoResponse>> list() { return ApiResponse.success(exhibitService.listInfo()); }

    @Operation(summary = "Tra cứu hiện vật và ngôn ngữ có bản dịch; không tạo audio")
    @GetMapping("/exhibits/{id}/info")
    public ApiResponse<ExhibitInfoResponse> info(@PathVariable Long id) { return ApiResponse.success(exhibitService.getInfo(id)); }

    @Operation(summary = "Danh sách ngôn ngữ VoiceRSS hỗ trợ", description = "Ngôn ngữ thực có của từng hiện vật nằm trong metadata hiện vật/QR.")
    @GetMapping("/languages")
    public ApiResponse<List<Languages.Language>> languages() { return ApiResponse.success(Languages.list()); }

    @Operation(summary = "Lấy bản dịch và audio theo ID hiện vật/ngôn ngữ")
    @GetMapping("/exhibits/{id}")
    public ApiResponse<ExhibitResponse> getExhibit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "vi-vn") String lang) {
        
        return ApiResponse.success(exhibitService.getExhibitByIdAndLang(id, lang));
    }
}
