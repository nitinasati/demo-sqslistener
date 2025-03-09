package com.learning.demo_sqslistener.exception;

import lombok.Getter;

/**
 * Custom exception class for handling message processing errors.
 * This exception extends the base RuntimeException class and provides
 * constructors for creating instances with a message and a cause.
 *
 * @author demo-sqslistener
 */
@Getter
public class MessageProcessingException extends RuntimeException {
    private final ErrorCodes errorCode;

    public MessageProcessingException(ErrorCodes errorCode) {
        super(errorCode.getFormattedMessage());
        this.errorCode = errorCode;
    }

    public MessageProcessingException(ErrorCodes errorCode, String additionalInfo) {
        super(errorCode.getFormattedMessage(additionalInfo));
        this.errorCode = errorCode;
    }

    public MessageProcessingException(ErrorCodes errorCode, Throwable cause) {
        super(errorCode.getFormattedMessage(), cause);
        this.errorCode = errorCode;
    }

    public MessageProcessingException(ErrorCodes errorCode, String additionalInfo, Throwable cause) {
        super(errorCode.getFormattedMessage(additionalInfo), cause);
        this.errorCode = errorCode;
    }
} 