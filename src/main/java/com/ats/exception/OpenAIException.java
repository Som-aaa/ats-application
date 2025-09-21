package com.ats.exception;

/**
 * Exception for OpenAI API operations
 */
public class OpenAIException extends ATSServiceException {
    
    public OpenAIException(String message) {
        super("OPENAI_API_ERROR", message, "Unable to process your request with AI service. Please try again later.");
    }

    public OpenAIException(String message, Throwable cause) {
        super("OPENAI_API_ERROR", message, "Unable to process your request with AI service. Please try again later.", cause);
    }
}
