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
import org.springframework.web.client.ResourceAccessException;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import com.learning.demo_sqslistener.exception.ErrorCodes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Message message;

    @Value("${api.endpoint.url}")
    private String apiUrl = "http://test-api.com";

    private MessageProcessor messageProcessor;

    @BeforeEach
    void setUp() {
        messageProcessor = new MessageProcessor(apiUrl);
        ReflectionTestUtils.setField(messageProcessor, "restTemplate", restTemplate);
    }

    @Test
    void processMessage_WithValidMessage_Success() {
        // Arrange
        when(message.getBody()).thenReturn("{\"name\":\"Test Product\",\"price\":99.99,\"quantity\":10}");
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
    @DisplayName("Should throw IllegalArgumentException when message body exceeds size limit")
    void processMessage_WithOversizedBody_ThrowsException() {
        // Arrange
        String largeBody = "x".repeat(MessageProcessor.MAX_MESSAGE_SIZE + 1);
        when(message.getBody()).thenReturn(largeBody);
        when(message.getMessageId()).thenReturn("test-id");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> messageProcessor.processMessage(message));
        assertTrue(exception.getMessage().contains("exceeds limit"));
    }

    @Test
    void processMessage_WithInvalidJson_ThrowsException() {
        // Arrange
        when(message.getBody()).thenReturn("{name: Test Product}");
        when(message.getMessageId()).thenReturn("test-message-id");

        // Act & Assert
        MessageProcessingException exception = assertThrows(
            MessageProcessingException.class,
            () -> messageProcessor.processMessage(message)
        );
        assertTrue(exception.getMessage().contains("Invalid JSON format"));
    }

    @Test
    @DisplayName("Should handle non-2xx API response")
    void processMessage_WithNon200Response_ThrowsException() {
        // Arrange
        String validJson = "{\"key\":\"value\"}";
        when(message.getBody()).thenReturn(validJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        // Act & Assert
        MessageProcessingException exception = assertThrows(MessageProcessingException.class,
            () -> messageProcessor.processMessage(message));
        assertEquals(ErrorCodes.API_RESPONSE_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle API connection timeout")
    void processMessage_WithApiTimeout_ThrowsException() {
        // Arrange
        String validJson = "{\"key\":\"value\"}";
        when(message.getBody()).thenReturn(validJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection timed out"));

        // Act & Assert
        MessageProcessingException exception = assertThrows(MessageProcessingException.class,
            () -> messageProcessor.processMessage(message));
        assertEquals(ErrorCodes.API_CONNECTION_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle empty but valid JSON")
    void processMessage_WithEmptyJson_Succeeds() {
        // Arrange
        String emptyJson = "{}";
        when(message.getBody()).thenReturn(emptyJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle message with special characters")
    void processMessage_WithSpecialCharacters_Succeeds() {
        // Arrange
        String jsonWithSpecialChars = "{\"key\":\"value with ñ and 漢字\"}";
        when(message.getBody()).thenReturn(jsonWithSpecialChars);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle invalid API URL")
    void constructor_WithInvalidUrl_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new MessageProcessor("invalid-url"));
    }

    @Test
    @DisplayName("Should handle null API URL")
    void constructor_WithNullUrl_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> new MessageProcessor(null));
    }

    @Test
    @DisplayName("Should handle deeply nested JSON")
    void processMessage_WithDeeplyNestedJson_Succeeds() {
        // Arrange
        String nestedJson = "{\"level1\":{\"level2\":{\"level3\":{\"key\":\"value\"}}}}";
        when(message.getBody()).thenReturn(nestedJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle message with maximum allowed size")
    void processMessage_WithMaxSizeMessage_Succeeds() {
        // Arrange
        String maxSizeJson = "{\"key\":\"" + "x".repeat(MessageProcessor.MAX_MESSAGE_SIZE - 10) + "\"}";
        when(message.getBody()).thenReturn(maxSizeJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle API server error response")
    void processMessage_WithServerError_ThrowsException() {
        // Arrange
        String validJson = "{\"key\":\"value\"}";
        when(message.getBody()).thenReturn(validJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        MessageProcessingException exception = assertThrows(MessageProcessingException.class,
            () -> messageProcessor.processMessage(message));
        assertEquals(ErrorCodes.API_RESPONSE_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle very small valid JSON")
    void processMessage_WithMinimalJson_Succeeds() {
        // Arrange
        String minimalJson = "{\"a\":1}";
        when(message.getBody()).thenReturn(minimalJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle JSON with array")
    void processMessage_WithJsonArray_Succeeds() {
        // Arrange
        String arrayJson = "{\"items\":[1,2,3,\"test\",true]}";
        when(message.getBody()).thenReturn(arrayJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle JSON with null values")
    void processMessage_WithNullValues_Succeeds() {
        // Arrange
        String jsonWithNull = "{\"key\":null,\"data\":\"value\"}";
        when(message.getBody()).thenReturn(jsonWithNull);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle API timeout with retry")
    void processMessage_WithApiTimeoutAndRetry_ThrowsException() {
        // Arrange
        String validJson = "{\"key\":\"value\"}";
        when(message.getBody()).thenReturn(validJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Read timed out"))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK)); // Second attempt would succeed

        // Act & Assert
        MessageProcessingException exception = assertThrows(MessageProcessingException.class,
            () -> messageProcessor.processMessage(message));
        assertEquals(ErrorCodes.API_CONNECTION_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle message with Unicode escape sequences")
    void processMessage_WithUnicodeEscapes_Succeeds() {
        // Arrange
        String unicodeJson = "{\"key\":\"\\u0048\\u0065\\u006C\\u006C\\u006F\"}"; // "Hello"
        when(message.getBody()).thenReturn(unicodeJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should handle message with escaped quotes")
    void processMessage_WithEscapedQuotes_Succeeds() {
        // Arrange
        String escapedJson = "{\"key\":\"value with \\\"quoted\\\" text\"}";
        when(message.getBody()).thenReturn(escapedJson);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> messageProcessor.processMessage(message));
    }

    @Test
    @DisplayName("Should verify sanitization of content")
    void processMessage_VerifySanitization() {
        // Arrange
        String jsonWithScript = "{\"key\":\"<script>alert('xss')</script>\"}";
        String expectedSanitized = "{\"key\":\"\"}"; // After removing script tags
        when(message.getBody()).thenReturn(jsonWithScript);
        when(message.getMessageId()).thenReturn("test-id");
        when(restTemplate.postForEntity(eq(apiUrl), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act
        messageProcessor.processMessage(message);

        // Assert
        verify(restTemplate).postForEntity(eq(apiUrl), eq(expectedSanitized), eq(String.class));
    }
} 