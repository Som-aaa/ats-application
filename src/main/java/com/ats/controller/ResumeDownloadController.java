package com.ats.controller;

import com.ats.model.ResumeMatch;
import com.ats.service.FileStorageService;
import com.ats.service.ResumeMatchManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for downloading stored resume files
 */
@RestController
@RequestMapping("/api/resume-download")
@CrossOrigin(origins = "*")
public class ResumeDownloadController {

    @Autowired
    private ResumeMatchManager resumeMatchManager;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Download the best match resume for a specific JD with custom naming
     */
    @GetMapping("/best-match/{jdIndex}")
    public ResponseEntity<Resource> downloadBestMatch(
            @PathVariable int jdIndex,
            @RequestParam String companyName,
            @RequestParam String roleName,
            @RequestParam(required = false) String userName) {
        
        try {
            System.out.println("🚀 DOWNLOAD REQUEST - JD: " + jdIndex);
            System.out.println("   Company: " + companyName);
            System.out.println("   Role: " + roleName);
            System.out.println("   User: " + userName);
            
            // Get the best match for this JD
            ResumeMatch bestMatch = resumeMatchManager.getBestMatchForJD(jdIndex);
            if (bestMatch == null) {
                System.out.println("❌ No best match found for JD: " + jdIndex);
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("✅ Found best match: " + bestMatch.getResumeFileName());
            System.out.println("   Stored as: " + bestMatch.getStoredResumeFilename());
            
            // Get the stored file path
            Path storedFilePath = fileStorageService.getStoredFilePath(bestMatch.getStoredResumeFilename());
            if (!Files.exists(storedFilePath)) {
                System.out.println("❌ Stored file not found: " + storedFilePath);
                return ResponseEntity.notFound().build();
            }
            
            // Generate new filename
            String newFileName = generateFileName(companyName, roleName, userName);
            String originalExtension = getFileExtension(bestMatch.getResumeFileName());
            if (!newFileName.toLowerCase().endsWith(originalExtension.toLowerCase())) {
                newFileName = newFileName + originalExtension;
            }
            
            System.out.println("📝 New filename: " + newFileName);
            
            // Create FileSystemResource
            FileSystemResource fileResource = new FileSystemResource(storedFilePath.toFile());
            
            // Set headers for download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(Files.size(storedFilePath));
            
            // Set content disposition with proper encoding
            String encodedFileName = URLEncoder.encode(newFileName, StandardCharsets.UTF_8).replace("+", "%20");
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + newFileName + "\"; filename*=UTF-8''" + encodedFileName);
            
            System.out.println("✅ Download ready: " + newFileName);
            System.out.println("   File size: " + Files.size(storedFilePath) + " bytes");
            
            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download a specific stored resume by filename
     */
    @GetMapping("/file/{storedFilename}")
    public ResponseEntity<Resource> downloadStoredFile(@PathVariable String storedFilename) {
        try {
            System.out.println("📁 DOWNLOAD REQUEST - File: " + storedFilename);
            
            // Check if file exists
            if (!fileStorageService.fileExists(storedFilename)) {
                System.out.println("❌ File not found: " + storedFilename);
                return ResponseEntity.notFound().build();
            }
            
            // Get file path and info
            Path filePath = fileStorageService.getStoredFilePath(storedFilename);
            long fileSize = fileStorageService.getFileSize(storedFilename);
            
            System.out.println("✅ File found: " + filePath);
            System.out.println("   Size: " + fileSize + " bytes");
            
            // Create FileSystemResource
            FileSystemResource fileResource = new FileSystemResource(filePath.toFile());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(fileSize);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + storedFilename + "\"");
            
            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get storage information
     */
    @GetMapping("/storage-info")
    public ResponseEntity<String> getStorageInfo() {
        try {
            String info = resumeMatchManager.getStorageInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get storage info: " + e.getMessage());
        }
    }

    /**
     * Generate filename based on company, role, and user
     */
    private String generateFileName(String companyName, String roleName, String userName) {
        StringBuilder fileName = new StringBuilder();
        
        if (companyName != null && !companyName.trim().isEmpty()) {
            fileName.append(companyName.trim().replaceAll("[^a-zA-Z0-9\\s\\-_]", "_"));
        }
        
        if (roleName != null && !roleName.trim().isEmpty()) {
            if (fileName.length() > 0) fileName.append("_");
            fileName.append(roleName.trim().replaceAll("[^a-zA-Z0-9\\s\\-_]", "_"));
        }
        
        if (userName != null && !userName.trim().isEmpty()) {
            if (fileName.length() > 0) fileName.append("_");
            fileName.append(userName.trim().replaceAll("[^a-zA-Z0-9\\s\\-_]", "_"));
        }
        
        if (fileName.length() == 0) {
            fileName.append("resume");
        }
        
        return fileName.toString();
    }

    /**
     * Helper method to extract file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
