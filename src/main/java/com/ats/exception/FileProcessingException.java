package com.ats.exception;

/**
 * Exception for file processing operations
 */
public class FileProcessingException extends ATSServiceException {
    
    public FileProcessingException(String message) {
        super("FILE_PROCESSING_ERROR", message, "Unable to process the uploaded file. Please check the file format and try again.");
    }

    public FileProcessingException(String message, Throwable cause) {
        super("FILE_PROCESSING_ERROR", message, "Unable to process the uploaded file. Please check the file format and try again.", cause);
    }
}
