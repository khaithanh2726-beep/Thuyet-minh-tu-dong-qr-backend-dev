package com.qraudio.backend.controller;

import com.qraudio.backend.service.AudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audio")
@Tag(name = "Public - Audio")
public class AudioController {
    private final AudioService service;
    public AudioController(AudioService service) { this.service = service; }
    @Operation(summary = "Phát MP3", description = "Lấy fileName từ audioUrl của API resolve; bấm Execute để hiện thanh phát audio trong Swagger.")
    @ApiResponse(responseCode = "200", description = "Audio MP3", content = @Content(mediaType = "audio/mpeg", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/play/{fileName}")
    public ResponseEntity<Resource> playAudio(@PathVariable String fileName) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(service.getAudio(fileName));
    }
}
