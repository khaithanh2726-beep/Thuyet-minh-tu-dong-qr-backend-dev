package com.qraudio.backend.controller;
import com.qraudio.backend.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class HealthController {
    private final HealthService service;
    public HealthController(HealthService service) { this.service = service; }
    @GetMapping("/api/health") public ResponseEntity<Map<String, String>> health() {
        boolean ready = service.ready();
        return ResponseEntity.status(ready ? 200 : 503).body(Map.of("status", ready ? "UP" : "DOWN"));
    }
}
