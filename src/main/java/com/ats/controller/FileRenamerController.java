package com.ats.controller;

import com.ats.service.FileRenamerService;
import com.ats.service.FileRenamerService.FileRenameResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for file renaming operations in the ATS application
 */
@RestController
@RequestMapping("/api/file-renamer")
@CrossOrigin(origins = "*")
public class FileRenamerController {

    @Autowired
    private FileRenamerService fileRenamerService;

    /**
     * Process and rename a file with company and role information
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyName") String companyName,
            @RequestParam("roleName") String roleName,
            @RequestParam(value = "userName", required = false) String userName) {
        
        try {
            System.out.println("DEBUG - File renaming request received");
            System.out.println("DEBUG - Original filename: " + file.getOriginalFilename());
            System.out.println("DEBUG - Company: " + companyName);
            System.out.println("DEBUG - Role: " + roleName);
            System.out.println("DEBUG - User: " + userName);
            
            // Process the file
            FileRenameResult result = fileRenamerService.processAndRenameFile(file, companyName, roleName, userName);
            
            // Debug logging for processing
            System.out.println("DEBUG - FileRenamerController: File processed successfully");
            System.out.println("DEBUG - FileRenamerController: New filename: " + result.getNewFileName());
            System.out.println("DEBUG - FileRenamerController: File size: " + result.getFileSize() + " bytes");
            System.out.println("DEBUG - FileRenamerController: Actual bytes: " + result.getFileBytes().length + " bytes");
            
            // Verify file integrity after processing
            if (result.getFileBytes().length != result.getFileSize()) {
                System.out.println("WARNING - FileRenamerController: Size mismatch after processing! Expected: " + result.getFileSize() + ", Actual: " + result.getFileBytes().length);
            }
            
            // Check file headers after processing
            if (result.getFileBytes().length > 4) {
                byte[] header = new byte[4];
                System.arraycopy(result.getFileBytes(), 0, header, 0, 4);
                System.out.println("DEBUG - FileRenamerController: Processed file header: " + String.format("%02X %02X %02X %02X", header[0], header[1], header[2], header[3]));
            }
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File processed successfully");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG - Validation error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (IOException e) {
            System.out.println("DEBUG - IO error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "File processing error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } catch (Exception e) {
            System.out.println("DEBUG - Unexpected error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * SUPER SIMPLE TEST - Just return the file directly without any processing
     */
    @PostMapping("/test-simple")
    public ResponseEntity<Resource> testSimpleFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("🧪 SUPER SIMPLE TEST STARTED");
            System.out.println("File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
            
            // Create temp file with timestamp
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFileName = "simple_test_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path tempFile = Paths.get(tempDir, tempFileName);
            
            System.out.println("Creating temp file: " + tempFile.toString());
            
            // Direct copy without any processing
            try (InputStream inputStream = file.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                outputStream.flush();
                System.out.println("File copied: " + totalBytes + " bytes");
            }
            
            // Verify size
            long tempFileSize = Files.size(tempFile);
            System.out.println("Temp file size: " + tempFileSize + " bytes");
            
            if (tempFileSize != file.getSize()) {
                System.out.println("❌ SIZE MISMATCH! Expected: " + file.getSize() + ", Got: " + tempFileSize);
                Files.deleteIfExists(tempFile);
                throw new RuntimeException("File corruption detected!");
            }
            
            // Create resource
            FileSystemResource fileResource = new FileSystemResource(tempFile.toFile());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            headers.setContentLength(tempFileSize);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + file.getOriginalFilename() + "\"");
            
            // Cleanup
            tempFile.toFile().deleteOnExit();
            
            System.out.println("✅ SUPER SIMPLE TEST READY: " + file.getOriginalFilename());
            
            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
                    
        } catch (Exception e) {
            System.out.println("❌ SUPER SIMPLE TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BULLETPROOF DOWNLOAD - Direct file streaming without corruption
     */
    @PostMapping("/download")
    public ResponseEntity<Resource> downloadRenamedFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyName") String companyName,
            @RequestParam("roleName") String roleName,
            @RequestParam(value = "userName", required = false) String userName) {
        
        try {
            System.out.println("🚨 BULLETPROOF DOWNLOAD STARTED");
            System.out.println("Original file: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
            
            // Generate new filename
            String newFileName = generateFileName(companyName, roleName, userName);
            String originalExtension = getFileExtension(file.getOriginalFilename());
            if (!newFileName.toLowerCase().endsWith(originalExtension.toLowerCase())) {
                newFileName = newFileName + originalExtension;
            }
            
            System.out.println("New filename: " + newFileName);
            
            // Create temporary file with UNIQUE name to avoid conflicts
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueFileName = "ats_" + System.currentTimeMillis() + "_" + newFileName;
            Path tempFile = Paths.get(tempDir, uniqueFileName);
            
            System.out.println("Creating temp file: " + tempFile.toString());
            
            // CRITICAL: Use direct file transfer without byte arrays
            try (InputStream inputStream = file.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
                
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                outputStream.flush();
                System.out.println("File written to temp: " + totalBytes + " bytes");
            }
            
            // Verify file integrity
            long tempFileSize = Files.size(tempFile);
            System.out.println("Temp file size: " + tempFileSize + " bytes");
            
            if (tempFileSize != file.getSize()) {
                System.out.println("❌ CRITICAL: Size mismatch! Expected: " + file.getSize() + ", Got: " + tempFileSize);
                Files.deleteIfExists(tempFile);
                throw new RuntimeException("File corruption detected during transfer!");
            }
            
            // Create FileSystemResource
            FileSystemResource fileResource = new FileSystemResource(tempFile.toFile());
            
            // Set headers for download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            headers.setContentLength(tempFileSize);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + newFileName + "\"");
            
            // Clean up temp file after response
            tempFile.toFile().deleteOnExit();
            
            System.out.println("✅ BULLETPROOF DOWNLOAD READY: " + newFileName);
            System.out.println("File size: " + tempFileSize + " bytes");
            
            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
                    
        } catch (Exception e) {
            System.out.println("❌ BULLETPROOF DOWNLOAD FAILED: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate filename without processing the file content
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
            fileName.append("renamed_file");
        }
        
        return fileName.toString();
    }

    /**
     * Get file information without processing
     */
    @PostMapping("/info")
    public ResponseEntity<Map<String, Object>> getFileInfo(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("originalFileName", file.getOriginalFilename());
            fileInfo.put("fileSize", file.getSize());
            fileInfo.put("contentType", file.getContentType());
            fileInfo.put("fileExtension", getFileExtension(file.getOriginalFilename()));
            
            return ResponseEntity.ok(fileInfo);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get file info: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test endpoint to return raw file without processing
     */
    @PostMapping("/test-raw")
    public ResponseEntity<byte[]> testRawFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("DEBUG - FileRenamerController: Testing raw file endpoint");
            System.out.println("DEBUG - FileRenamerController: Original filename: " + file.getOriginalFilename());
            System.out.println("DEBUG - FileRenamerController: Original size: " + file.getSize() + " bytes");
            
            // Read file bytes using the robust method
            byte[] fileBytes = file.getInputStream().readAllBytes();
            System.out.println("DEBUG - FileRenamerController: Read size: " + fileBytes.length + " bytes");
            
            // Verify integrity
            if (fileBytes.length != file.getSize()) {
                System.out.println("WARNING - FileRenamerController: Raw file size mismatch! Expected: " + file.getSize() + ", Got: " + fileBytes.length);
            }
            
            // Check file headers
            if (fileBytes.length > 4) {
                byte[] header = new byte[4];
                System.arraycopy(fileBytes, 0, header, 0, 4);
                System.out.println("DEBUG - FileRenamerController: Raw file header: " + String.format("%02X %02X %02X %02X", header[0], header[1], header[2], header[3]));
            }
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            headers.setContentDispositionFormData("attachment", "test-raw" + getFileExtension(file.getOriginalFilename()));
            headers.setContentLength(fileBytes.length);
            
            System.out.println("DEBUG - FileRenamerController: Returning raw file for testing");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
                    
        } catch (Exception e) {
            System.out.println("DEBUG - FileRenamerController: Raw file test failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Raw file test failed: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Minimal test endpoint - direct file return without any processing
     */
    @PostMapping("/test-direct")
    public ResponseEntity<byte[]> testDirectFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== DIRECT FILE TEST ===");
            System.out.println("Original filename: " + file.getOriginalFilename());
            System.out.println("Original size: " + file.getSize() + " bytes");
            System.out.println("Content type: " + file.getContentType());
            
            // Method 1: Try getBytes()
            byte[] bytes1 = file.getBytes();
            System.out.println("Method 1 (getBytes): " + bytes1.length + " bytes");
            
            // Method 2: Try getInputStream().readAllBytes()
            byte[] bytes2 = file.getInputStream().readAllBytes();
            System.out.println("Method 2 (getInputStream): " + bytes2.length + " bytes");
            
            // Method 3: Try reading in chunks
            byte[] bytes3 = new byte[(int) file.getSize()];
            int totalRead = 0;
            try (var inputStream = file.getInputStream()) {
                int bytesRead;
                while (totalRead < bytes3.length && (bytesRead = inputStream.read(bytes3, totalRead, bytes3.length - totalRead)) != -1) {
                    totalRead += bytesRead;
                }
            }
            System.out.println("Method 3 (chunked read): " + totalRead + " bytes");
            
            // Check if any method corrupted the file
            if (bytes1.length != file.getSize()) {
                System.out.println("WARNING: getBytes() corrupted the file!");
            }
            if (bytes2.length != file.getSize()) {
                System.out.println("WARNING: getInputStream() corrupted the file!");
            }
            if (totalRead != file.getSize()) {
                System.out.println("WARNING: Chunked read corrupted the file!");
            }
            
            // Check file headers for each method
            if (bytes1.length > 4) {
                byte[] header1 = new byte[4];
                System.arraycopy(bytes1, 0, header1, 0, 4);
                System.out.println("Method 1 header: " + String.format("%02X %02X %02X %02X", header1[0], header1[1], header1[2], header1[3]));
            }
            
            if (bytes2.length > 4) {
                byte[] header2 = new byte[4];
                System.arraycopy(bytes2, 0, header2, 0, 4);
                System.out.println("Method 2 header: " + String.format("%02X %02X %02X %02X", header2[0], header2[1], header2[2], header2[3]));
            }
            
            // Use the most reliable method for return
            byte[] finalBytes = bytes2.length == file.getSize() ? bytes2 : bytes1;
            
            System.out.println("Returning file with size: " + finalBytes.length + " bytes");
            System.out.println("=== END DIRECT FILE TEST ===");
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            headers.setContentDispositionFormData("attachment", "direct-test" + getFileExtension(file.getOriginalFilename()));
            headers.setContentLength(finalBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(finalBytes);
                    
        } catch (Exception e) {
            System.out.println("Direct file test failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Direct file test failed: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "FileRenamerService");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Helper method to extract file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        
        return "";
    }
}
