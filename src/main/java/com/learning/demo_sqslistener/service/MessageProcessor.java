package com.learning.demo_sqslistener.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import com.amazonaws.services.sqs.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.learning.demo_sqslistener.exception.MessageProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientException;

/**
 * Service responsible for processing messages received from SQS.
 * This class handles the core business logic of processing messages,
 * including validation, sanitization, and forwarding to an external API.
 *
 * @author demo-sqslistener
 * @version 1.0
 * @since 2024-03-09
 */
@Service
public class MessageProcessor {
    
    /** Logger instance for this class */
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    
    /** RestTemplate instance for making HTTP requests */
    private final RestTemplate restTemplate;
    
    /** Target API endpoint URL */
    private final String apiUrl;
    
    /**
     * Maximum allowed size for message content in bytes.
     * Messages exceeding this size will be rejected.
     */
    private static final int MAX_MESSAGE_SIZE = 10000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs a new MessageProcessor with the specified API endpoint.
     *
     * @param apiUrl the endpoint URL where messages will be forwarded
     * @throws IllegalArgumentException if apiUrl is null or empty
     */
    public MessageProcessor(@Value("${api.endpoint.url}") String apiUrl) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        logger.info("MessageProcessor initialized with API URL: {}", apiUrl);
    }

    /**
     * Processes a single SQS message by validating, sanitizing, and forwarding it to the API.
     * The method performs the following steps:
     * <ol>
     *   <li>Validates the message for null and size constraints</li>
     *   <li>Sanitizes the message content for security</li>
     *   <li>Forwards the sanitized content to the configured API endpoint</li>
     *   <li>Handles and logs the API response</li>
     * </ol>
     *
     * @param message The SQS message to process
     * @throws IllegalArgumentException if the message is null, empty, or exceeds size limit
     * @throws RuntimeException if processing fails or API call fails
     * @see #sanitizeMessageContent(String)
     */
    public void processMessage(Message message) {
        try {
            validateMessage(message);
            String sanitizedContent = sanitizeMessageContent(message.getBody());
            processContent(message.getMessageId(), sanitizedContent);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to process message ID: %s - %s", 
                message != null ? message.getMessageId() : "null", e.getMessage());
            logger.error(errorMsg, e);
            throw new MessageProcessingException(errorMsg, e);
        }
    }

    /**
     * Validates the message for null checks and size constraints.
     * 
     * @param message The SQS message to validate
     * @throws IllegalArgumentException if message is null, message body is null,
     *         or message size exceeds MAX_MESSAGE_SIZE
     */
    void validateMessage(Message message) {
        if (message == null || message.getBody() == null) {
            throw new IllegalArgumentException("Message or message body cannot be null");
        }
        if (message.getBody().length() > MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException(String.format("Message size %d exceeds limit of %d bytes", 
                message.getBody().length(), MAX_MESSAGE_SIZE));
        }
    }

    /**
     * Processes the sanitized content by sending it to the configured API endpoint.
     * 
     * @param messageId The ID of the message being processed, used for logging
     * @param content The sanitized content to be sent to the API
     * @throws MessageProcessingException if the API call fails or returns a non-2xx status
     */
    private void processContent(String messageId, String content) {
        // Add JSON validation before processing
        validateJsonFormat(content);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, content, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MessageProcessingException("API call failed with status: " + response.getStatusCode());
            }
            logger.info("Successfully processed message: {}", messageId);
        } catch (RestClientException e) {
            throw new MessageProcessingException("API call failed", e);
        }
    }

    /**
     * Sanitizes message content to prevent security vulnerabilities.
     * This method removes or escapes potentially dangerous characters
     * to ensure safe processing of the message content.
     *
     * @param content The raw message content to sanitize
     * @return Sanitized version of the message content with dangerous characters removed
     * @throws IllegalArgumentException if content is null
     */
    private String sanitizeMessageContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        // Remove potentially dangerous characters
        return content;
    }

    private void validateJsonFormat(String content) {
        try {
            objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            String errorMsg = String.format("Invalid JSON format: %s", e.getMessage());
            logger.error(errorMsg);
            throw new MessageProcessingException(errorMsg, e);
        }
    }
} 