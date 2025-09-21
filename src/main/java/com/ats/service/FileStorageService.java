package com.ats.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for storing and managing physical files
 */
@Service
public class FileStorageService {

    private static final String STORAGE_DIR = "resume_storage";
    private final Path storageLocation;

    public FileStorageService() {
        // Create storage directory in the application root
        this.storageLocation = Paths.get(STORAGE_DIR);
        try {
            if (!Files.exists(storageLocation)) {
                Files.createDirectories(storageLocation);
                System.out.println("✅ Created resume storage directory: " + storageLocation.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to create storage directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Store a resume file and return the storage path
     */
    public String storeResume(MultipartFile file) throws IOException {
        // Generate unique filename to avoid conflicts
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        Path targetPath = storageLocation.resolve(uniqueFilename);
        
        // Copy file to storage
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        System.out.println("✅ Resume stored: " + targetPath.toAbsolutePath());
        System.out.println("   Original: " + originalFilename);
        System.out.println("   Stored as: " + uniqueFilename);
        System.out.println("   Size: " + Files.size(targetPath) + " bytes");
        
        return uniqueFilename;
    }

    /**
     * Get the stored file path
     */
    public Path getStoredFilePath(String storedFilename) {
        return storageLocation.resolve(storedFilename);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String storedFilename) {
        Path filePath = getStoredFilePath(storedFilename);
        return Files.exists(filePath);
    }

    /**
     * Get file size
     */
    public long getFileSize(String storedFilename) throws IOException {
        Path filePath = getStoredFilePath(storedFilename);
        return Files.size(filePath);
    }

    /**
     * Delete stored file
     */
    public boolean deleteFile(String storedFilename) {
        try {
            Path filePath = getStoredFilePath(storedFilename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("✅ Deleted stored file: " + storedFilename);
                return true;
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to delete file: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get storage directory path
     */
    public Path getStorageLocation() {
        return storageLocation;
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
