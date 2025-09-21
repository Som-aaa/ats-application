package com.ats.controller;

import com.ats.service.ATSService;
import com.ats.utils.ValidationUtils;
import com.ats.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ATSController {

    private static final Logger logger = LoggerFactory.getLogger(ATSController.class);

    @Autowired
    private ATSService atsService;

    // Rate limiting: track requests per IP
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_HOUR = 10; // Adjust based on your needs
    private static final long HOUR_IN_MILLIS = 60 * 60 * 1000;

    @PostMapping("/mode1")
    public ResponseEntity<?> evaluateResume(
            @RequestParam("resume") @NotNull MultipartFile resume, 
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        logger.info("Mode 1 request received for file: {}", resume.getOriginalFilename());
        
        try {
            // Validate inputs
            ValidationUtils.validateFile(resume, "resume");
            ValidationUtils.validateClientId(clientId);
            
            // Rate limiting check
            String identifier = clientId != null ? clientId : "default";
            if (!checkRateLimit(identifier)) {
                logger.warn("Rate limit exceeded for identifier: {}", identifier);
                throw new ValidationException("Rate limit exceeded. Please try again later.");
            }

            Map<String, Object> result = atsService.evaluateResumeMode1(resume);
            logger.info("Mode 1 analysis completed successfully for file: {}", resume.getOriginalFilename());
            return ResponseEntity.ok(result);
            
        } catch (ValidationException e) {
            logger.warn("Validation error in mode 1: {}", e.getMessage());
            throw e; // Let GlobalExceptionHandler handle it
        } catch (Exception e) {
            logger.error("Unexpected error in mode 1 for file: {}", resume.getOriginalFilename(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @PostMapping(value = "/mode2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> evaluateResumeAndJD(
            @RequestParam("resume") @NotNull MultipartFile resume,
            @RequestParam("jd") @NotBlank String jdText,
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        logger.info("Mode 2 request received for file: {} with JD length: {}", 
            resume.getOriginalFilename(), jdText.length());
        
        try {
            // Validate inputs
            ValidationUtils.validateFile(resume, "resume");
            ValidationUtils.validateText(jdText, "job description", true);
            ValidationUtils.validateClientId(clientId);
            
            // Rate limiting check
            String identifier = clientId != null ? clientId : "default";
            if (!checkRateLimit(identifier)) {
                logger.warn("Rate limit exceeded for identifier: {}", identifier);
                throw new ValidationException("Rate limit exceeded. Please try again later.");
            }

            Map<String, Object> result = atsService.evaluateResumeWithJDText(resume, jdText);
            logger.info("Mode 2 analysis completed successfully for file: {}", resume.getOriginalFilename());
            return ResponseEntity.ok(result);
            
        } catch (ValidationException e) {
            logger.warn("Validation error in mode 2: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in mode 2 for file: {}", resume.getOriginalFilename(), e);
            throw e;
        }
    }

    @PostMapping(value = "/mode3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkResumeAnalysis(
            @RequestParam("resumes") @NotNull MultipartFile[] resumes,
            @RequestParam("jd") @NotBlank String jdText,
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        logger.info("Mode 3 request received for {} files with JD length: {}", 
            resumes.length, jdText.length());
        
        try {
            // Validate inputs
            ValidationUtils.validateFiles(resumes, "resumes");
            ValidationUtils.validateText(jdText, "job description", true);
            ValidationUtils.validateClientId(clientId);
            
            // Additional validation for bulk operations
            if (resumes.length > 10) {
                throw new ValidationException("Maximum 10 resumes allowed per analysis");
            }
            
            // Rate limiting check
            String identifier = clientId != null ? clientId : "default";
            if (!checkRateLimit(identifier)) {
                logger.warn("Rate limit exceeded for identifier: {}", identifier);
                throw new ValidationException("Rate limit exceeded. Please try again later.");
            }

            Map<String, Object> result = atsService.bulkResumeAnalysis(resumes, jdText);
            logger.info("Mode 3 analysis completed successfully for {} files", resumes.length);
            return ResponseEntity.ok(result);
            
        } catch (ValidationException e) {
            logger.warn("Validation error in mode 3: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in mode 3 for {} files", resumes.length, e);
            throw e;
        }
    }

    @PostMapping(value = "/mode4", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkJDResumeAnalysis(
            @RequestParam("resumes") @NotNull MultipartFile[] resumes,
            @RequestParam("jobDescriptions") @NotNull MultipartFile jdFile,
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        logger.info("Mode 4 request received for {} files with JD file: {}", 
            resumes.length, jdFile.getOriginalFilename());
        
        try {
            // Validate inputs
            ValidationUtils.validateFiles(resumes, "resumes");
            ValidationUtils.validateFile(jdFile, "job descriptions file");
            ValidationUtils.validateClientId(clientId);
            
            // Additional validation for bulk operations
            if (resumes.length > 20) {
                throw new ValidationException("Maximum 20 resumes allowed per analysis");
            }
            
            // Validate Excel file type
            String fileName = jdFile.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                throw new ValidationException("Only Excel files (.xlsx, .xls) are supported for job descriptions");
            }
            
            // Rate limiting check
            String identifier = clientId != null ? clientId : "default";
            if (!checkRateLimit(identifier)) {
                logger.warn("Rate limit exceeded for identifier: {}", identifier);
                throw new ValidationException("Rate limit exceeded. Please try again later.");
            }

            Map<String, Object> result = atsService.bulkJDResumeAnalysis(resumes, jdFile);
            logger.info("Mode 4 analysis completed successfully for {} files", resumes.length);
            return ResponseEntity.ok(result);
            
        } catch (ValidationException e) {
            logger.warn("Validation error in mode 4: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in mode 4 for {} files", resumes.length, e);
            throw e;
        }
    }

    private boolean checkRateLimit(String identifier) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRequestTime.get(identifier);
        
        // Reset counter if an hour has passed
        if (lastTime == null || currentTime - lastTime > HOUR_IN_MILLIS) {
            requestCounts.put(identifier, new AtomicInteger(1));
            lastRequestTime.put(identifier, currentTime);
            logger.debug("Rate limit reset for identifier: {}", identifier);
            return true;
        }
        
        // Check if limit exceeded
        AtomicInteger count = requestCounts.get(identifier);
        if (count == null) {
            count = new AtomicInteger(0);
            requestCounts.put(identifier, count);
        }
        
        int currentCount = count.incrementAndGet();
        logger.debug("Rate limit check for identifier: {} - current count: {}/{}", 
            identifier, currentCount, MAX_REQUESTS_PER_HOUR);
        
        if (currentCount > MAX_REQUESTS_PER_HOUR) {
            logger.warn("Rate limit exceeded for identifier: {} - count: {}", identifier, currentCount);
            return false;
        }
        
        return true;
    }

    // Endpoint to check remaining requests
    @GetMapping("/rate-limit-status")
    public ResponseEntity<?> getRateLimitStatus(@RequestParam(value = "clientId", required = false) String clientId) {
        try {
            ValidationUtils.validateClientId(clientId);
            
            String identifier = clientId != null ? clientId : "default";
            AtomicInteger count = requestCounts.get(identifier);
            int used = count != null ? count.get() : 0;
            
            logger.debug("Rate limit status requested for identifier: {} - used: {}/{}", 
                identifier, used, MAX_REQUESTS_PER_HOUR);
            
            return ResponseEntity.ok(Map.of(
                "used", used,
                "limit", MAX_REQUESTS_PER_HOUR,
                "remaining", Math.max(0, MAX_REQUESTS_PER_HOUR - used)
            ));
        } catch (ValidationException e) {
            logger.warn("Validation error in rate limit status: {}", e.getMessage());
            throw e;
        }
    }

    // Endpoint to clear cache (for debugging)
    @PostMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            logger.info("Cache clear request received");
            atsService.clearCache();
            logger.info("Cache cleared successfully");
            return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
        } catch (Exception e) {
            logger.error("Failed to clear cache", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    // Endpoint to get cache status
    @GetMapping("/cache-status")
    public ResponseEntity<?> getCacheStatus() {
        try {
            logger.debug("Cache status request received");
            Map<String, Object> status = atsService.getCacheStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get cache status", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
    
}
