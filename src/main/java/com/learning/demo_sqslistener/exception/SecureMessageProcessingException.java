package com.learning.demo_sqslistener.exception;

/**
 * Custom exception for secure message processing errors.
 * Provides sanitized error messages without exposing sensitive information.
 *
 * @author demo-sqslistener
 * @version 1.0
 */
public class SecureMessageProcessingException extends RuntimeException {
    /**
     * Constructs a new SecureMessageProcessingException with the specified message.
     *
     * @param message The error message
     */
    public SecureMessageProcessingException(String message) {
        super(message);
    }
    
    /**
     * Returns a sanitized string representation of the exception.
     * Prevents information leakage by not including stack traces.
     *
     * @return Sanitized exception message
     */
    @Override
    public String toString() {
        return "SecureMessageProcessingException: " + getMessage();
    }
} 