package com.qraudio.backend.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
    private final String AUDIO_DIR = "audio_cache/";

    @GetMapping("/play/{fileName}")
    public ResponseEntity<Resource> playAudio(@PathVariable String fileName) {
        try {
            // Tìm file trong thư mục audio_cache
            Path filePath = Paths.get(AUDIO_DIR).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                // Trả về file bản dịch
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}