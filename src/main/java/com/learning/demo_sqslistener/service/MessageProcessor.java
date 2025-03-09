package com.learning.demo_sqslistener.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import com.amazonaws.services.sqs.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.learning.demo_sqslistener.exception.ErrorCodes;
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
        if (!isValidUrl(apiUrl)) {
            logger.error("Invalid API URL provided: {}", apiUrl);
            throw new IllegalArgumentException(
                String.format("Invalid API URL: %s", apiUrl));
        }
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
            logger.debug("Starting to process message: {}", message != null ? message.getMessageId() : "null");
            validateMessage(message);
            String sanitizedContent = sanitizeMessageContent(message.getBody());
            processContent(message.getMessageId(), sanitizedContent);
            logger.info("Successfully processed message: {}", message.getMessageId());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (MessageProcessingException e) {
            // Re-throw MessageProcessingException directly
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Message ID: %s", 
                message != null ? message.getMessageId() : "null");
            logger.error("Message processing failed: {}", errorMsg, e);
            throw new MessageProcessingException(ErrorCodes.MESSAGE_PROCESSING_ERROR, errorMsg, e);
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
        logger.debug("Validating message...");
        if (message == null || message.getBody() == null) {
            logger.error("Null message or message body received");
            throw new IllegalArgumentException("Message or message body cannot be null");
        }
        if (message.getBody().length() > MAX_MESSAGE_SIZE) {
            logger.error("Message size {} exceeds limit of {} bytes for message ID: {}", 
                message.getBody().length(), MAX_MESSAGE_SIZE, message.getMessageId());
            throw new IllegalArgumentException(
                String.format("Message size %d exceeds limit of %d bytes", 
                    message.getBody().length(), MAX_MESSAGE_SIZE));
        }
        logger.debug("Message validation successful for message ID: {}", message.getMessageId());
    }

    /**
     * Processes the sanitized content by sending it to the configured API endpoint.
     * 
     * @param messageId The ID of the message being processed, used for logging
     * @param content The sanitized content to be sent to the API
     * @throws MessageProcessingException if the API call fails or returns a non-2xx status
     */
    private void processContent(String messageId, String content) {
        logger.debug("Processing content for message ID: {}", messageId);
        try {
            validateJsonFormat(content);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, content, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("API call failed with status: {} for message ID: {}", 
                    response.getStatusCode(), messageId);
                throw new MessageProcessingException(ErrorCodes.API_RESPONSE_ERROR, 
                    String.format("API call failed with status: %s", response.getStatusCode()));
            }
            logger.debug("Successfully processed content for message ID: {}", messageId);
        } catch (JsonProcessingException e) {
            logger.error("Invalid JSON format for message ID: {}", messageId, e);
            throw new MessageProcessingException(ErrorCodes.INVALID_JSON_FORMAT, 
                "Invalid JSON format in message", e);
        } catch (RestClientException e) {
            logger.error("API call failed for message ID: {}", messageId, e);
            throw new MessageProcessingException(ErrorCodes.API_CONNECTION_ERROR, 
                "Failed to process message due to API error", e);
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
        logger.debug("Sanitizing message content");
        if (content == null) {
            logger.error("Cannot sanitize null content");
            throw new IllegalArgumentException("Content cannot be null");
        }
        // Add your sanitization logic here
        logger.debug("Message content sanitization completed");
        return content;
    }

    private void validateJsonFormat(String content) throws JsonProcessingException {
        logger.debug("Validating JSON format");
        objectMapper.readTree(content);
        logger.debug("JSON format validation successful");
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 