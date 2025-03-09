package com.learning.demo_sqslistener.exception;

import lombok.Getter;

@Getter
public enum ErrorCodes {
    // SQS Related Errors (1000-1999)
    SQS_CONNECTION_ERROR("SQS-1001", "Failed to connect to SQS service"),
    SQS_MESSAGE_SEND_ERROR("SQS-1002", "Failed to send message to SQS queue"),
    SQS_MESSAGE_DELETE_ERROR("SQS-1003", "Failed to delete message from SQS queue"),
    SQS_MESSAGE_RECEIVE_ERROR("SQS-1004", "Failed to receive messages from SQS queue"),
    SQS_VISIBILITY_UPDATE_ERROR("SQS-1005", "Failed to update message visibility timeout"),
    SQS_DLQ_MOVE_ERROR("SQS-1006", "Failed to move message to Dead Letter Queue"),
    
    // Message Processing Errors (2000-2999)
    MESSAGE_PROCESSING_ERROR("MSG-2001", "Failed to process message"),
    MESSAGE_VALIDATION_ERROR("MSG-2002", "Message validation failed"),
    MESSAGE_SIZE_EXCEEDED("MSG-2003", "Message size exceeds maximum limit"),
    MESSAGE_FORMAT_ERROR("MSG-2004", "Invalid message format"),
    INVALID_JSON_FORMAT("MSG-2005", "Invalid JSON format in message"),
    
    // Retry Management Errors (3000-3999)
    RETRY_LIMIT_EXCEEDED("RTY-3001", "Maximum retry attempts exceeded"),
    RETRY_COUNT_UPDATE_ERROR("RTY-3002", "Failed to update retry count"),
    
    // API Related Errors (4000-4999)
    API_CONNECTION_ERROR("API-4001", "Failed to connect to external API"),
    API_TIMEOUT_ERROR("API-4002", "API request timed out"),
    API_RESPONSE_ERROR("API-4003", "Invalid response from API"),
    
    // Configuration Errors (5000-5999)
    CONFIG_MISSING_ERROR("CFG-5001", "Required configuration is missing"),
    CONFIG_INVALID_ERROR("CFG-5002", "Invalid configuration value"),
    
    // General System Errors (9000-9999)
    SYSTEM_ERROR("SYS-9001", "Internal system error"),
    UNEXPECTED_ERROR("SYS-9002", "An unexpected error occurred");

    private final String code;
    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getFormattedMessage() {
        return String.format("[%s] %s", code, message);
    }

    public String getFormattedMessage(String additionalInfo) {
        return String.format("[%s] %s - %s", code, message, additionalInfo);
    }
} 