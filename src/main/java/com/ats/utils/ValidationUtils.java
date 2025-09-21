package com.ats.utils;

import com.ats.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtils {
    
    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_TEXT_LENGTH = 100000; // 100k characters
    private static final int MAX_FILES_PER_REQUEST = 20;
    
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\p{S}]*$");
    private static final Pattern XSS_PATTERN = Pattern.compile(".*<script.*>.*</script>.*|.*javascript:.*|.*on\\w+\\s*=.*", Pattern.CASE_INSENSITIVE);
    
    /**
     * Validates a multipart file
     */
    public static void validateFile(MultipartFile file, String fieldName) throws ValidationException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(fieldName + " is required");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds maximum allowed limit of 10MB");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new ValidationException("Unsupported file type. Only PDF, DOC, DOCX, XLS, and XLSX files are allowed");
        }
        
        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ValidationException("Invalid file name");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("File extension not allowed. Only .pdf, .doc, .docx, .xls, .xlsx files are supported");
        }
    }
    
    /**
     * Validates multiple files
     */
    public static void validateFiles(MultipartFile[] files, String fieldName) throws ValidationException {
        if (files == null || files.length == 0) {
            throw new ValidationException(fieldName + " is required");
        }
        
        if (files.length > MAX_FILES_PER_REQUEST) {
            throw new ValidationException("Too many files. Maximum " + MAX_FILES_PER_REQUEST + " files allowed per request");
        }
        
        for (int i = 0; i < files.length; i++) {
            try {
                validateFile(files[i], fieldName + "[" + i + "]");
            } catch (ValidationException e) {
                throw new ValidationException("File " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Validates text content
     */
    public static void validateText(String text, String fieldName, boolean required) throws ValidationException {
        if (required && (text == null || text.trim().isEmpty())) {
            throw new ValidationException(fieldName + " is required");
        }
        
        if (text != null) {
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new ValidationException(fieldName + " exceeds maximum length of " + MAX_TEXT_LENGTH + " characters");
            }
            
            // Check for XSS patterns
            if (XSS_PATTERN.matcher(text).matches()) {
                throw new ValidationException("Invalid content detected in " + fieldName);
            }
            
            // Check for safe text pattern
            if (!SAFE_TEXT_PATTERN.matcher(text).matches()) {
                throw new ValidationException("Invalid characters detected in " + fieldName);
            }
        }
    }
    
    /**
     * Sanitizes text content
     */
    public static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        
        // Remove potential XSS content
        text = text.replaceAll("(?i)<script.*?>.*?</script>", "");
        text = text.replaceAll("(?i)javascript:", "");
        text = text.replaceAll("(?i)on\\w+\\s*=", "");
        
        // Normalize whitespace
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    /**
     * Validates client ID
     */
    public static void validateClientId(String clientId) throws ValidationException {
        if (clientId != null && !clientId.trim().isEmpty()) {
            if (clientId.length() > 100) {
                throw new ValidationException("Client ID too long");
            }
            
            if (!clientId.matches("^[a-zA-Z0-9_-]+$")) {
                throw new ValidationException("Invalid client ID format");
            }
        }
    }
    
    /**
     * Gets file extension from filename
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
