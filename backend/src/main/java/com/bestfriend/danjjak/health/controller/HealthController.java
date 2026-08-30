package com.bestfriend.danjjak.health.controller;

import com.bestfriend.danjjak.health.service.HealthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> databaseHealth() {
        String database = healthService.isDatabaseReady() ? "UP" : "DOWN";
        return ResponseEntity.ok(Map.of("status", "UP", "database", database));
    }
}

