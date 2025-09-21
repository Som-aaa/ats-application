package com.ats.exception;

/**
 * Exception for input validation errors
 */
public class ValidationException extends ATSServiceException {
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, message);
    }

    public ValidationException(String message, String userMessage) {
        super("VALIDATION_ERROR", message, userMessage);
    }
}
