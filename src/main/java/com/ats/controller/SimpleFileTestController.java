package com.ats.controller;

import com.ats.service.FileStorageService;
import com.ats.service.RenamedFileStorageService;
import com.ats.service.ResumeMatchManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple test controller to verify file storage and renaming works
 */
@RestController
@RequestMapping("/api/simple-test")
@CrossOrigin(origins = "*")
public class SimpleFileTestController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RenamedFileStorageService renamedFileStorageService;

    @Autowired
    private ResumeMatchManager resumeMatchManager;

    /**
     * Simple file upload and storage test
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyName") String companyName,
            @RequestParam("roleName") String roleName,
            @RequestParam(value = "userName", required = false) String userName) {
        
        try {
            System.out.println("🧪 SIMPLE UPLOAD TEST STARTED");
            System.out.println("File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
            System.out.println("Company: " + companyName);
            System.out.println("Role: " + roleName);
            System.out.println("User: " + userName);
            
            // Store the original file
            String storedFilename = fileStorageService.storeResume(file);
            
            // Store the renamed file
            String renamedFilename = renamedFileStorageService.storeRenamedResume(file, companyName, roleName, userName);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File stored and renamed successfully");
            response.put("originalFilename", file.getOriginalFilename());
            response.put("storedFilename", storedFilename);
            response.put("renamedFilename", renamedFilename);
            response.put("fileSize", file.getSize());
            response.put("companyName", companyName);
            response.put("roleName", roleName);
            response.put("userName", userName);
            
            System.out.println("✅ File stored and renamed successfully!");
            System.out.println("   Original stored as: " + storedFilename);
            System.out.println("   Renamed stored as: " + renamedFilename);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Download renamed file directly
     */
    @GetMapping("/download-renamed/{renamedFilename}")
    public ResponseEntity<Resource> downloadRenamedFile(@PathVariable String renamedFilename) {
        
        try {
            System.out.println("📥 DOWNLOADING RENAMED FILE: " + renamedFilename);
            
            // Check if renamed file exists
            if (!renamedFileStorageService.renamedFileExists(renamedFilename)) {
                System.out.println("❌ Renamed file not found: " + renamedFilename);
                return ResponseEntity.notFound().build();
            }
            
            // Get file path and info
            Path filePath = renamedFileStorageService.getRenamedFilePath(renamedFilename);
            long fileSize = renamedFileStorageService.getRenamedFileSize(renamedFilename);
            
            System.out.println("✅ Renamed file found: " + filePath);
            System.out.println("Size: " + fileSize + " bytes");
            
            // Create FileSystemResource
            FileSystemResource fileResource = new FileSystemResource(filePath.toFile());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(fileSize);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + renamedFilename + "\"");
            
            System.out.println("✅ Download ready: " + renamedFilename);
            
            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * List all renamed files
     */
    @GetMapping("/list-renamed-files")
    public ResponseEntity<Map<String, Object>> listRenamedFiles() {
        try {
            System.out.println("📋 LISTING RENAMED FILES");
            
            String[] renamedFiles = renamedFileStorageService.listRenamedFiles();
            String storageInfo = renamedFileStorageService.getRenamedFilesLocation().toString();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("renamedFilesDirectory", storageInfo);
            response.put("renamedFiles", renamedFiles);
            response.put("totalFiles", renamedFiles.length);
            response.put("message", "Renamed files listed successfully");
            
            // List files in console
            System.out.println("Renamed files directory: " + renamedFileStorageService.getRenamedFilesLocation().toAbsolutePath());
            System.out.println("Total renamed files: " + renamedFiles.length);
            for (String file : renamedFiles) {
                System.out.println("  - " + file);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ List renamed files failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "List renamed files failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * List all stored files (original)
     */
    @GetMapping("/list-stored-files")
    public ResponseEntity<Map<String, Object>> listStoredFiles() {
        try {
            System.out.println("📋 LISTING STORED FILES");
            
            String storageInfo = fileStorageService.getStorageLocation().toString();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storedFilesDirectory", storageInfo);
            response.put("message", "Check console for stored files listing");
            
            // List files in console
            System.out.println("Stored files directory: " + fileStorageService.getStorageLocation().toAbsolutePath());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ List stored files failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "List stored files failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create renamed file from existing stored file
     */
    @PostMapping("/create-renamed")
    public ResponseEntity<Map<String, Object>> createRenamedFile(
            @RequestParam("storedFilename") String storedFilename,
            @RequestParam("companyName") String companyName,
            @RequestParam("roleName") String roleName,
            @RequestParam(value = "userName", required = false) String userName) {
        
        try {
            System.out.println("🔄 CREATING RENAMED FILE FROM STORED");
            System.out.println("Stored file: " + storedFilename);
            System.out.println("Company: " + companyName);
            System.out.println("Role: " + roleName);
            System.out.println("User: " + userName);
            
            // Check if stored file exists
            if (!fileStorageService.fileExists(storedFilename)) {
                System.out.println("❌ Stored file not found: " + storedFilename);
                return ResponseEntity.notFound().build();
            }
            
            // Create renamed file
            String renamedFilename = renamedFileStorageService.storeRenamedResumeFromStored(
                storedFilename, companyName, roleName, userName, fileStorageService);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Renamed file created successfully");
            response.put("storedFilename", storedFilename);
            response.put("renamedFilename", renamedFilename);
            response.put("companyName", companyName);
            response.put("roleName", roleName);
            response.put("userName", userName);
            
            System.out.println("✅ Renamed file created: " + renamedFilename);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Create renamed file failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Create renamed file failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test the ATS workflow integration
     */
    @PostMapping("/test-ats-workflow")
    public ResponseEntity<Map<String, Object>> testATSWorkflow(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("jdIndex") int jdIndex) {
        
        try {
            System.out.println("🧪 TESTING ATS WORKFLOW INTEGRATION");
            System.out.println("File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
            System.out.println("JD: " + jobDescription.substring(0, Math.min(100, jobDescription.length())) + "...");
            System.out.println("JD Index: " + jdIndex);
            
            // Simulate the ATS workflow by calling the same method
            // This will create both stored and renamed files
            resumeMatchManager.storeResumeMatch(
                jdIndex,
                jobDescription,
                file.getOriginalFilename(),
                file.getOriginalFilename(),
                8.5, // Mock ATS score
                file
            );
            
            // Check if files were created
            String[] renamedFiles = renamedFileStorageService.listRenamedFiles();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ATS workflow test completed");
            response.put("originalFile", file.getOriginalFilename());
            response.put("jobDescription", jobDescription.substring(0, Math.min(100, jobDescription.length())) + "...");
            response.put("jdIndex", jdIndex);
            response.put("renamedFilesCreated", renamedFiles.length);
            response.put("renamedFilesList", renamedFiles);
            
            System.out.println("✅ ATS workflow test completed!");
            System.out.println("   Renamed files created: " + renamedFiles.length);
            for (String renamedFile : renamedFiles) {
                System.out.println("     - " + renamedFile);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ATS workflow test failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "ATS workflow test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
