package com.ats.controller;

import com.ats.service.ATSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private ATSService atsService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Basic health check
            status.put("status", "UP");
            status.put("timestamp", System.currentTimeMillis());
            status.put("service", "ATS Application");
            status.put("version", "1.0.0");
            
            // Check cache status
            Map<String, Object> cacheStatus = atsService.getCacheStatus();
            status.put("cache", cacheStatus);
            
            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("total", runtime.totalMemory());
            memory.put("free", runtime.freeMemory());
            memory.put("used", runtime.totalMemory() - runtime.freeMemory());
            memory.put("max", runtime.maxMemory());
            status.put("memory", memory);
            
            logger.debug("Health check completed successfully");
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(status);
        }
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        Map<String, Object> readiness = new HashMap<>();
        
        try {
            // Check if service is ready to accept requests
            readiness.put("ready", true);
            readiness.put("timestamp", System.currentTimeMillis());
            
            // Check critical dependencies
            Map<String, Object> dependencies = new HashMap<>();
            dependencies.put("cache", atsService.getCacheStatus());
            dependencies.put("apiKey", checkApiKey());
            readiness.put("dependencies", dependencies);
            
            logger.debug("Readiness check completed successfully");
            return ResponseEntity.ok(readiness);
            
        } catch (Exception e) {
            logger.error("Readiness check failed", e);
            readiness.put("ready", false);
            readiness.put("error", e.getMessage());
            readiness.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(readiness);
        }
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        Map<String, Object> liveness = new HashMap<>();
        
        try {
            // Simple liveness check
            liveness.put("alive", true);
            liveness.put("timestamp", System.currentTimeMillis());
            liveness.put("uptime", System.currentTimeMillis() - getStartTime());
            
            return ResponseEntity.ok(liveness);
            
        } catch (Exception e) {
            logger.error("Liveness check failed", e);
            liveness.put("alive", false);
            liveness.put("error", e.getMessage());
            liveness.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(liveness);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getBasicHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "ATS Application is running successfully!");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "ATS Application");
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> checkApiKey() {
        Map<String, Object> apiKeyStatus = new HashMap<>();
        try {
            String apiKey = atsService.getApiKeyForHealthCheck();
            apiKeyStatus.put("configured", apiKey != null && !apiKey.equals("your-openai-api-key-here"));
            apiKeyStatus.put("length", apiKey != null ? apiKey.length() : 0);
        } catch (Exception e) {
            apiKeyStatus.put("configured", false);
            apiKeyStatus.put("error", e.getMessage());
        }
        return apiKeyStatus;
    }

    private static long startTime = System.currentTimeMillis();
    
    private long getStartTime() {
        return startTime;
    }
}