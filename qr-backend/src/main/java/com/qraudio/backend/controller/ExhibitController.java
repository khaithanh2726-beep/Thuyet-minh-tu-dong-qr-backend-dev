package com.qraudio.backend.controller;

import com.qraudio.backend.dto.ApiResponse;
import com.qraudio.backend.dto.ExhibitResponse;
import com.qraudio.backend.service.ExhibitService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exhibits")
public class ExhibitController {
    private final ExhibitService exhibitService;

    public ExhibitController(ExhibitService exhibitService) {
        this.exhibitService = exhibitService;
    }

    // GET: /api/exhibits/{id}
    @GetMapping("/{id}")
    public ApiResponse<ExhibitResponse> getExhibit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "vi-vn") String lang) {
        
        ExhibitResponse data = exhibitService.getExhibitByIdAndLang(id, lang);
        return ApiResponse.success(data);
    }
}