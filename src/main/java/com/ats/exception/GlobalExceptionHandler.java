package com.ats.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ATSServiceException.class)
    public ResponseEntity<Map<String, Object>> handleATSServiceException(ATSServiceException ex) {
        logger.error("ATS Service Error: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("message", ex.getUserMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        HttpStatus status = determineHttpStatus(ex);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        logger.warn("Validation Error: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("message", ex.getUserMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleFileProcessingException(FileProcessingException ex) {
        logger.error("File Processing Error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("message", ex.getUserMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(OpenAIException.class)
    public ResponseEntity<Map<String, Object>> handleOpenAIException(OpenAIException ex) {
        logger.error("OpenAI API Error: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("message", ex.getUserMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.warn("File size exceeded: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", "FILE_SIZE_EXCEEDED");
        errorResponse.put("message", "File size exceeds the maximum allowed limit. Please upload a smaller file.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Validation errors: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", "VALIDATION_ERROR");
        errorResponse.put("message", "Validation failed");
        errorResponse.put("errors", errors);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", "INTERNAL_ERROR");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private HttpStatus determineHttpStatus(ATSServiceException ex) {
        return switch (ex.getErrorCode()) {
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "FILE_PROCESSING_ERROR" -> HttpStatus.BAD_REQUEST;
            case "OPENAI_API_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
