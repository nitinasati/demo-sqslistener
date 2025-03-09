package com.learning.demo_sqslistener.exception;
/**
 * Custom exception class for handling message processing errors.
 * This exception extends the base RuntimeException class and provides
 * constructors for creating instances with a message and a cause.
 *
 * @author demo-sqslistener
 */
public class MessageProcessingException extends RuntimeException {
    public MessageProcessingException(String message) {
        super(message);
    }
    
    public MessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
} 