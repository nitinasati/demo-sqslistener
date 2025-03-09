package com.learning.demo_sqslistener.exception;

import lombok.Getter;

@Getter
public class SQSProcessingException extends RuntimeException {
    private final ErrorCodes errorCode;

    public SQSProcessingException(ErrorCodes errorCode) {
        super(errorCode.getFormattedMessage());
        this.errorCode = errorCode;
    }

    public SQSProcessingException(ErrorCodes errorCode, String additionalInfo) {
        super(errorCode.getFormattedMessage(additionalInfo));
        this.errorCode = errorCode;
    }

    public SQSProcessingException(ErrorCodes errorCode, Throwable cause) {
        super(errorCode.getFormattedMessage(), cause);
        this.errorCode = errorCode;
    }

    public SQSProcessingException(ErrorCodes errorCode, String additionalInfo, Throwable cause) {
        super(errorCode.getFormattedMessage(additionalInfo), cause);
        this.errorCode = errorCode;
    }
} 