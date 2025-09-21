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

/**
 * Service for storing renamed resume files in a dedicated folder
 */
@Service
public class RenamedFileStorageService {

    private static final String RENAMED_FILES_DIR = "renamed_resumes";
    private final Path renamedFilesLocation;

    public RenamedFileStorageService() {
        // Create renamed files directory in the application root
        this.renamedFilesLocation = Paths.get(RENAMED_FILES_DIR);
        try {
            if (!Files.exists(renamedFilesLocation)) {
                Files.createDirectories(renamedFilesLocation);
                System.out.println("✅ Created renamed resumes directory: " + renamedFilesLocation.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to create renamed resumes directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Store a renamed resume file
     */
    public String storeRenamedResume(MultipartFile originalFile, String companyName, String roleName, String userName) throws IOException {
        // Generate new filename
        String newFileName = generateFileName(companyName, roleName, userName);
        String originalExtension = getFileExtension(originalFile.getOriginalFilename());
        if (!newFileName.toLowerCase().endsWith(originalExtension.toLowerCase())) {
            newFileName = newFileName + originalExtension;
        }
        
        // Add timestamp to avoid conflicts
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String finalFileName = timestamp + "_" + newFileName;
        
        // Create the file path
        Path targetPath = renamedFilesLocation.resolve(finalFileName);
        
        // Copy the file with new name
        Files.copy(originalFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        System.out.println("✅ Renamed resume stored: " + targetPath.toAbsolutePath());
        System.out.println("   Original: " + originalFile.getOriginalFilename());
        System.out.println("   Renamed as: " + finalFileName);
        System.out.println("   Size: " + Files.size(targetPath) + " bytes");
        
        return finalFileName;
    }

    /**
     * Store a renamed resume from an existing stored file
     */
    public String storeRenamedResumeFromStored(String storedFilename, String companyName, String roleName, String userName, 
                                             FileStorageService fileStorageService) throws IOException {
        // Get the original stored file
        Path originalFilePath = fileStorageService.getStoredFilePath(storedFilename);
        if (!Files.exists(originalFilePath)) {
            throw new IOException("Original file not found: " + storedFilename);
        }
        
        // Generate new filename
        String newFileName = generateFileName(companyName, roleName, userName);
        String originalExtension = getFileExtension(storedFilename);
        if (!newFileName.toLowerCase().endsWith(originalExtension.toLowerCase())) {
            newFileName = newFileName + originalExtension;
        }
        
        // Add timestamp to avoid conflicts
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String finalFileName = timestamp + "_" + newFileName;
        
        // Create the file path
        Path targetPath = renamedFilesLocation.resolve(finalFileName);
        
        // Copy the file with new name
        Files.copy(originalFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        System.out.println("✅ Renamed resume stored from stored file: " + targetPath.toAbsolutePath());
        System.out.println("   Original stored: " + storedFilename);
        System.out.println("   Renamed as: " + finalFileName);
        System.out.println("   Size: " + Files.size(targetPath) + " bytes");
        
        return finalFileName;
    }

    /**
     * Get the renamed file path
     */
    public Path getRenamedFilePath(String renamedFilename) {
        return renamedFilesLocation.resolve(renamedFilename);
    }

    /**
     * Check if renamed file exists
     */
    public boolean renamedFileExists(String renamedFilename) {
        Path filePath = getRenamedFilePath(renamedFilename);
        return Files.exists(filePath);
    }

    /**
     * Get renamed file size
     */
    public long getRenamedFileSize(String renamedFilename) throws IOException {
        Path filePath = getRenamedFilePath(renamedFilename);
        return Files.size(filePath);
    }

    /**
     * List all renamed files
     */
    public String[] listRenamedFiles() {
        try {
            return Files.list(renamedFilesLocation)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toArray(String[]::new);
        } catch (IOException e) {
            System.err.println("❌ Failed to list renamed files: " + e.getMessage());
            return new String[0];
        }
    }

    /**
     * Delete a renamed file
     */
    public boolean deleteRenamedFile(String renamedFilename) {
        try {
            Path filePath = getRenamedFilePath(renamedFilename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("✅ Deleted renamed file: " + renamedFilename);
                return true;
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to delete renamed file: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get renamed files directory path
     */
    public Path getRenamedFilesLocation() {
        return renamedFilesLocation;
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
