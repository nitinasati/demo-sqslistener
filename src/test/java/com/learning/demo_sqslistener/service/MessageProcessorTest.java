package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.model.Message;
import com.learning.demo_sqslistener.exception.MessageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private MessageProcessor messageProcessor;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Message message;

    private static final String API_URL = "http://test-api.com";
    private static final String VALID_JSON = "{\"name\":\"Test Product\",\"price\":99.99,\"quantity\":10}";
    private static final String INVALID_JSON = "{name: Test Product}";

    @BeforeEach
    void setUp() {
        messageProcessor = new MessageProcessor(API_URL);
        ReflectionTestUtils.setField(messageProcessor, "restTemplate", restTemplate);
    }

    @Test
    void processMessage_WithValidMessage_Success() {
        // Arrange
        when(message.getBody()).thenReturn(VALID_JSON);
        when(message.getMessageId()).thenReturn("test-message-id");
        when(restTemplate.postForEntity(anyString(), any(), any()))
            .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    void processMessage_WithNullMessage_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> messageProcessor.validateMessage(null)
        );
        assertEquals("Message or message body cannot be null", exception.getMessage());
    }

    @Test
    void processMessage_WithNullBody_ThrowsException() {
        // Arrange
        when(message.getBody()).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> messageProcessor.validateMessage(message)
        );
        assertEquals("Message or message body cannot be null", exception.getMessage());
    }

    @Test
    void processMessage_WithOversizedMessage_ThrowsException() {
        // Arrange
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 11000; i++) {
            largeContent.append("a");
        }
        when(message.getBody()).thenReturn(largeContent.toString());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> messageProcessor.validateMessage(message)
        );
        assertTrue(exception.getMessage().contains("Message size"));
    }

    @Test
    void processMessage_WithInvalidJson_ThrowsException() {
        // Arrange
        when(message.getBody()).thenReturn(INVALID_JSON);
        when(message.getMessageId()).thenReturn("test-message-id");

        // Act & Assert
        MessageProcessingException exception = assertThrows(
            MessageProcessingException.class,
            () -> messageProcessor.processMessage(message)
        );
        assertTrue(exception.getMessage().contains("Invalid JSON format"));
    }

    @Test
    void processMessage_WithApiError_ThrowsException() {
        // Arrange
        when(message.getBody()).thenReturn(VALID_JSON);
        when(message.getMessageId()).thenReturn("test-message-id");
        when(restTemplate.postForEntity(anyString(), any(), any()))
            .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        MessageProcessingException exception = assertThrows(
            MessageProcessingException.class,
            () -> messageProcessor.processMessage(message)
        );
        assertTrue(exception.getMessage().contains("API call failed with status"));
    }

    @Test
    void processMessage_WithSpecialCharacters_Success() {
        // Arrange
        String jsonWithSpecialChars = "{\"name\":\"Test & Product\",\"description\":\"Test < > Product\"}";
        when(message.getBody()).thenReturn(jsonWithSpecialChars);
        when(message.getMessageId()).thenReturn("test-message-id");
        when(restTemplate.postForEntity(anyString(), any(), any()))
            .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    void processMessage_WithEmptyButValidJson_Success() {
        // Arrange
        String emptyJson = "{}";
        when(message.getBody()).thenReturn(emptyJson);
        when(message.getMessageId()).thenReturn("test-message-id");
        when(restTemplate.postForEntity(anyString(), any(), any()))
            .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }
} 