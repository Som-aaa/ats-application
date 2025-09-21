package com.ats.exception;

/**
 * Base exception for ATS service operations
 */
public class ATSServiceException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;

    public ATSServiceException(String message) {
        super(message);
        this.errorCode = "ATS_ERROR";
        this.userMessage = message;
    }

    public ATSServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ATS_ERROR";
        this.userMessage = message;
    }

    public ATSServiceException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public ATSServiceException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
