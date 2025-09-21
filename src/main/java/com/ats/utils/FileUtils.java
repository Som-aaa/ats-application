package com.ats.utils;

import com.ats.exception.FileProcessingException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private static final Tika TIKA_INSTANCE = new Tika();
    
    public static String extractText(MultipartFile file) throws FileProcessingException {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("File is null or empty");
        }
        
        logger.debug("Extracting text from file: {} (size: {} bytes, type: {})", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        try (InputStream inputStream = file.getInputStream()) {
            String extractedText = TIKA_INSTANCE.parseToString(inputStream);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new FileProcessingException("No text content could be extracted from the file");
            }
            
            logger.debug("Text extraction successful. Extracted {} characters", extractedText.length());
            logger.trace("Text preview: {}", 
                extractedText.substring(0, Math.min(100, extractedText.length())) + "...");
            
            return extractedText.trim();
            
        } catch (IOException e) {
            logger.error("IO error while extracting text from file: {}", file.getOriginalFilename(), e);
            throw new FileProcessingException("Unable to read file content", e);
        } catch (TikaException e) {
            logger.error("Tika error while extracting text from file: {}", file.getOriginalFilename(), e);
            throw new FileProcessingException("Unable to parse file content. Please ensure the file is not corrupted", e);
        } catch (Exception e) {
            logger.error("Unexpected error while extracting text from file: {}", file.getOriginalFilename(), e);
            throw new FileProcessingException("Unexpected error while processing file", e);
        }
    }
    
    /**
     * Safely extracts text with fallback error handling
     */
    public static String extractTextSafely(MultipartFile file) {
        try {
            return extractText(file);
        } catch (FileProcessingException e) {
            logger.warn("Failed to extract text from file: {}", file.getOriginalFilename(), e);
            return "Error extracting text: " + e.getMessage();
        }
    }
}
