package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    private DeadLetterQueueService dlqService;

    @Mock
    private AmazonSQS amazonSQS;

    @Mock
    private Message message;

    private static final String QUEUE_URL = "queue-url";
    private static final String DLQ_URL = "dlq-url";

    @BeforeEach
    void setUp() {
        dlqService = new DeadLetterQueueService(amazonSQS, QUEUE_URL, DLQ_URL);
    }

    @Test
    void moveMessageToDLQ_Success() {
        when(message.getMessageId()).thenReturn("test-id");
        when(message.getBody()).thenReturn("test-body");
        when(message.getReceiptHandle()).thenReturn("test-receipt");

        dlqService.moveMessageToDLQ(message, "Test failure reason");

        ArgumentCaptor<SendMessageRequest> sendMessageCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSQS).sendMessage(sendMessageCaptor.capture());
        verify(amazonSQS).deleteMessage(QUEUE_URL, "test-receipt");

        SendMessageRequest capturedRequest = sendMessageCaptor.getValue();
        assertEquals(DLQ_URL, capturedRequest.getQueueUrl());
        assertEquals("test-body", capturedRequest.getMessageBody());
        assertEquals("test-id", capturedRequest.getMessageAttributes().get("OriginalMessageId").getStringValue());
        assertEquals("Test failure reason", capturedRequest.getMessageAttributes().get("FailureReason").getStringValue());
    }

    @Test
    void moveMessageToDLQ_WhenSendFails_DoesNotDelete() {
        when(message.getMessageId()).thenReturn("test-id");
        when(message.getBody()).thenReturn("test-body");
        when(amazonSQS.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(new RuntimeException("Send failed"));

        dlqService.moveMessageToDLQ(message, "Test failure reason");

        verify(amazonSQS, never()).deleteMessage(anyString(), anyString());
    }
} 