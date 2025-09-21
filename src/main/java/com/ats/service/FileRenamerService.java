package com.ats.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for renaming and processing files in the ATS application
 */
@Service
public class FileRenamerService {

    /**
     * Process and rename a file with company and role information
     */
    public FileRenameResult processAndRenameFile(MultipartFile file, String companyName, String roleName, String userName) throws IOException {
        
        // Validate inputs
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }
        
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name is required");
        }
        
        // Generate new filename
        String newFileName = generateNewFileName(file.getOriginalFilename(), companyName, roleName, userName);
        
        // Get file extension
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        // Create result object
        FileRenameResult result = new FileRenameResult();
        result.setOriginalFileName(file.getOriginalFilename());
        result.setNewFileName(newFileName);
        result.setFileSize(file.getSize());
        result.setContentType(file.getContentType());
        result.setFileExtension(fileExtension);
        result.setCompanyName(companyName);
        result.setRoleName(roleName);
        result.setUserName(userName);
        result.setProcessedAt(LocalDateTime.now());
        
        // Store file bytes for download
        byte[] fileBytes = file.getInputStream().readAllBytes();
        result.setFileBytes(fileBytes);
        
        // Debug logging for file integrity
        System.out.println("DEBUG - FileRenamerService: File processed successfully");
        System.out.println("DEBUG - FileRenamerService: Original size: " + file.getSize() + " bytes");
        System.out.println("DEBUG - FileRenamerService: Read size: " + fileBytes.length + " bytes");
        System.out.println("DEBUG - FileRenamerService: New filename: " + newFileName);
        
        // Verify file integrity
        if (fileBytes.length != file.getSize()) {
            System.out.println("WARNING - FileRenamerService: Size mismatch! Expected: " + file.getSize() + ", Got: " + fileBytes.length);
        }
        
        // Check file headers for corruption
        if (fileBytes.length > 4) {
            byte[] header = new byte[4];
            System.arraycopy(fileBytes, 0, header, 0, 4);
            System.out.println("DEBUG - FileRenamerService: File header bytes: " + String.format("%02X %02X %02X %02X", header[0], header[1], header[2], header[3]));
            
            // PDF header: %PDF
            if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
                System.out.println("DEBUG - FileRenamerService: Valid PDF header detected");
            }
            // DOC header: D0CF11E0
            else if (header[0] == (byte)0xD0 && header[1] == (byte)0xCF && header[2] == 0x11 && header[3] == (byte)0xE0) {
                System.out.println("DEBUG - FileRenamerService: Valid DOC header detected");
            }
            // DOCX header: PK (ZIP format)
            else if (header[0] == 0x50 && header[1] == 0x4B) {
                System.out.println("DEBUG - FileRenamerService: Valid DOCX header detected");
            }
            else {
                System.out.println("DEBUG - FileRenamerService: File header: " + String.format("%02X %02X %02X %02X", header[0], header[1], header[2], header[3]));
            }
        }
        
        return result;
    }
    
    /**
     * Generate a new filename based on company, role, and user information
     */
    private String generateNewFileName(String originalFileName, String companyName, String roleName, String userName) {
        StringBuilder newName = new StringBuilder();
        
        // Add company name
        if (companyName != null && !companyName.trim().isEmpty()) {
            newName.append(sanitizeFileName(companyName.trim()));
        }
        
        // Add role name
        if (roleName != null && !roleName.trim().isEmpty()) {
            if (newName.length() > 0) {
                newName.append("_");
            }
            newName.append(sanitizeFileName(roleName.trim()));
        }
        
        // Add user name if provided
        if (userName != null && !userName.trim().isEmpty()) {
            if (newName.length() > 0) {
                newName.append("_");
            }
            newName.append(sanitizeFileName(userName.trim()));
        }
        
        // Add timestamp if no user name provided (to ensure uniqueness)
        if (userName == null || userName.trim().isEmpty()) {
            if (newName.length() > 0) {
                newName.append("_");
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            newName.append(timestamp);
        }
        
        // Add file extension
        String extension = getFileExtension(originalFileName);
        if (extension != null && !extension.isEmpty()) {
            newName.append(extension);
        }
        
        return newName.toString();
    }
    
    /**
     * Sanitize filename by removing invalid characters
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "";
        
        // Replace invalid characters with underscores
        return fileName.replaceAll("[<>:\"/|?*\\\\]", "_")
                      .replaceAll("\\s+", "_")  // Replace spaces with underscores
                      .replaceAll("_+", "_")    // Replace multiple underscores with single
                      .trim();
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        
        return "";
    }
    
    /**
     * Generate a unique identifier for the file
     */
    public String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Result class for file renaming operations
     */
    public static class FileRenameResult {
        private String originalFileName;
        private String newFileName;
        private long fileSize;
        private String contentType;
        private String fileExtension;
        private String companyName;
        private String roleName;
        private String userName;
        private LocalDateTime processedAt;
        private byte[] fileBytes;
        private String uniqueId;
        
        // Constructors
        public FileRenameResult() {
            this.uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        
        // Getters and Setters
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        
        public String getNewFileName() { return newFileName; }
        public void setNewFileName(String newFileName) { this.newFileName = newFileName; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
        
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
        
        public byte[] getFileBytes() { return fileBytes; }
        public void setFileBytes(byte[] fileBytes) { this.fileBytes = fileBytes; }
        
        public String getUniqueId() { return uniqueId; }
        public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
        
        @Override
        public String toString() {
            return "FileRenameResult{" +
                    "originalFileName='" + originalFileName + '\'' +
                    ", newFileName='" + newFileName + '\'' +
                    ", fileSize=" + fileSize +
                    ", contentType='" + contentType + '\'' +
                    ", companyName='" + companyName + '\'' +
                    ", roleName='" + roleName + '\'' +
                    ", userName='" + userName + '\'' +
                    ", processedAt=" + processedAt +
                    ", uniqueId='" + uniqueId + '\'' +
                    '}';
        }
    }
}
