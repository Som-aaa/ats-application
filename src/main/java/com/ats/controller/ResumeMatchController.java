package com.ats.controller;

import com.ats.model.ResumeMatch;
import com.ats.service.ResumeMatchManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Simplified controller for displaying resume matches
 */
@RestController
@RequestMapping("/api/resume-matches")
@CrossOrigin(origins = "*")
public class ResumeMatchController {

    @Autowired
    private ResumeMatchManager resumeMatchManager;

    /**
     * Manually set resume content for testing
     */
    @PostMapping("/set-content/{matchId}")
    public ResponseEntity<Map<String, Object>> setResumeContent(
            @PathVariable String matchId,
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Content is required"));
            }
            
            // Convert string content to bytes for storage
            byte[] resumeFile = content.getBytes("UTF-8");
            
            boolean success = resumeMatchManager.setResumeContent(matchId, resumeFile);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "Resume content set successfully",
                    "matchId", matchId,
                    "contentLength", resumeFile.length
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test endpoint to get raw resume content
     */
    @GetMapping("/test-content/{jdIndex}")
    public ResponseEntity<String> testResumeContent(@PathVariable int jdIndex) {
        try {
            ResumeMatch bestMatch = resumeMatchManager.getBestMatchForJD(jdIndex);
            if (bestMatch == null) {
                return ResponseEntity.notFound().build();
            }
            
            String content = "RESUME MATCH FOUND:\n\n" + 
                           "Match ID: " + bestMatch.getMatchId() + "\n" +
                           "Resume File: " + bestMatch.getResumeFileName() + "\n" +
                           "Stored as: " + bestMatch.getStoredResumeFilename() + "\n" +
                           "Original name: " + bestMatch.getOriginalResumeName() + "\n" +
                           "Score: " + bestMatch.getMatchScore();
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ERROR: " + e.getMessage());
        }
    }

    /**
     * Debug endpoint to check resume content storage
     */
    @GetMapping("/debug/{jdIndex}")
    public ResponseEntity<Map<String, Object>> debugResumeContent(@PathVariable int jdIndex) {
        try {
            ResumeMatch bestMatch = resumeMatchManager.getBestMatchForJD(jdIndex);
            if (bestMatch == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("matchId", bestMatch.getMatchId());
            debugInfo.put("resumeFileName", bestMatch.getResumeFileName());
            debugInfo.put("storedResumeFilename", bestMatch.getStoredResumeFilename());
            debugInfo.put("originalResumeName", bestMatch.getOriginalResumeName());
            debugInfo.put("matchScore", bestMatch.getMatchScore());
            debugInfo.put("jobDescription", bestMatch.getJobDescription());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all resume matches
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllResumeMatches() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("matches", resumeMatchManager.getAllMatches());
            response.put("message", "All matches retrieved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve matches: " + e.getMessage()));
        }
    }

    /**
     * Get resume matches for a specific job description
     */
    @GetMapping("/job-description/{jdIndex}")
    public ResponseEntity<List<ResumeMatch>> getResumeMatchesForJD(@PathVariable int jdIndex) {
        try {
            List<ResumeMatch> matches = resumeMatchManager.getMatchesForJD(jdIndex);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get best match for a specific job description
     */
    @GetMapping("/job-description/{jdIndex}/best")
    public ResponseEntity<ResumeMatch> getBestMatchForJD(@PathVariable int jdIndex) {
        try {
            ResumeMatch bestMatch = resumeMatchManager.getBestMatchForJD(jdIndex);
            if (bestMatch != null) {
                return ResponseEntity.ok(bestMatch);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download best match for a specific job description
     */
    @GetMapping("/job-description/{jdIndex}/download-best")
    public ResponseEntity<byte[]> downloadBestMatchForJD(@PathVariable int jdIndex) {
        try {
            System.out.println("DEBUG - Download request for JD: " + jdIndex);
            
            // Get the file content directly from the service
            byte[] fileContent = resumeMatchManager.downloadBestMatchForJD(jdIndex);
            System.out.println("DEBUG - File content retrieved, size: " + fileContent.length + " bytes");
            
            // Get the match details for filename
            ResumeMatch match = resumeMatchManager.getBestMatchForJD(jdIndex);
            if (match == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Generate the filename with proper extension
            String filename = match.getOriginalResumeName();
            if (filename == null || filename.trim().isEmpty()) {
                filename = match.getResumeFileName();
            }
            
            // Ensure proper file extension
            String fileExtension = getFileExtension(match.getResumeFileName());
            if (!filename.toLowerCase().endsWith(fileExtension.toLowerCase())) {
                filename += fileExtension;
            }
            
            System.out.println("DEBUG - Downloading file: " + filename);
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileContent.length);
            
            System.out.println("DEBUG - Returning file with headers: " + headers);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
                    
        } catch (IllegalStateException e) {
            System.out.println("DEBUG - Download failed (no match): " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.out.println("DEBUG - Download failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Download failed: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        
        return "";
    }

    /**
     * Get all best matches across all job descriptions
     */
    @GetMapping("/best-matches")
    public ResponseEntity<List<ResumeMatch>> getAllBestMatches() {
        try {
            List<ResumeMatch> bestMatches = resumeMatchManager.getAllBestMatches();
            return ResponseEntity.ok(bestMatches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get matched resumes (score >= 6.0)
     */
    @GetMapping("/matched")
    public ResponseEntity<List<ResumeMatch>> getMatchedResumes() {
        try {
            List<ResumeMatch> matchedResumes = resumeMatchManager.getMatchedResumes();
            return ResponseEntity.ok(matchedResumes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unmatched resumes (score < 6.0)
     */
    @GetMapping("/unmatched")
    public ResponseEntity<List<ResumeMatch>> getUnmatchedResumes() {
        try {
            List<ResumeMatch> unmatchedResumes = resumeMatchManager.getUnmatchedResumes();
            return ResponseEntity.ok(unmatchedResumes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get resume match by ID
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<ResumeMatch> getResumeMatch(@PathVariable String matchId) {
        try {
            ResumeMatch match = resumeMatchManager.getResumeMatch(matchId);
            if (match != null) {
                return ResponseEntity.ok(match);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear all matches
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAllMatches() {
        try {
            resumeMatchManager.clearAllMatches();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = resumeMatchManager.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search resume matches
     */
    @GetMapping("/search")
    public ResponseEntity<List<ResumeMatch>> searchResumeMatches(@RequestParam String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<ResumeMatch> results = resumeMatchManager.searchResumeMatches(query.trim());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get matches by score range
     */
    @GetMapping("/score-range")
    public ResponseEntity<List<ResumeMatch>> getMatchesByScoreRange(
            @RequestParam double minScore, 
            @RequestParam double maxScore) {
        try {
            if (minScore < 0 || maxScore > 10 || minScore > maxScore) {
                return ResponseEntity.badRequest().build();
            }
            
            List<ResumeMatch> results = resumeMatchManager.getMatchesByScoreRange(minScore, maxScore);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
