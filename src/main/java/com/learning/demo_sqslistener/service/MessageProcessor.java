package com.learning.demo_sqslistener.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import com.amazonaws.services.sqs.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for processing messages received from SQS.
 * Handles message validation, sanitization, and API communication.
 *
 * @author demo-sqslistener
 * @version 1.0
 */
@Service
public class MessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    private final RestTemplate restTemplate;
    private final String apiUrl;
    /**
     * Maximum allowed size for message content in bytes
     */
    private static final int MAX_MESSAGE_SIZE = 10000; // Assuming a default max size

    public MessageProcessor(@Value("${api.endpoint.url}") String apiUrl) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        logger.info("MessageProcessor initialized with API URL: {}", apiUrl);
    }

    /**
     * Processes a single SQS message.
     * Validates the message, converts it to a Product object, and sends it to the API.
     *
     * @param message The SQS message to process
     * @throws IllegalArgumentException if the message is invalid
     * @throws RuntimeException if processing fails
     */
    public void processMessage(Message message) {
        if (message == null || message.getBody() == null) {
            logger.error("Invalid message received");
            throw new IllegalArgumentException("Message or message body cannot be null");
        }
        
        // Add message size validation
        if (message.getBody().length() > MAX_MESSAGE_SIZE) {
            logger.error("Message exceeds maximum size limit");
            throw new IllegalArgumentException("Message size exceeds limit");
        }
        
        // Sanitize message content before processing
        String sanitizedContent = sanitizeMessageContent(message.getBody());
        
        try {
            logger.debug("Processing message with ID: {}", message.getMessageId());
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, sanitizedContent, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("API call successful. Response: {}", response.getBody());
            } else {
                logger.warn("API call failed with status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to process message: {}", message.getMessageId(), e);
            throw new RuntimeException("Failed to process message", e);
        }
    }

    /**
     * Sanitizes message content to prevent security vulnerabilities.
     *
     * @param content The raw message content
     * @return Sanitized message content
     */
    private String sanitizeMessageContent(String content) {
        // Implement your sanitization logic here
        return content; // Placeholder return, actual implementation needed
    }
} 