package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private AmazonSQS amazonSQS;

    private DeadLetterQueueService dlqService;
    private static final String DLQ_URL = "dlq-url";
    private static final String QUEUE_URL = "queue-url";

    @BeforeEach
    void setUp() {
        dlqService = new DeadLetterQueueService(amazonSQS, DLQ_URL, QUEUE_URL);
    }

    @Test
    void moveMessageToDLQ_SuccessfulMove() {
        Message message = new Message()
            .withMessageId("test-message-id")
            .withBody("test-body")
            .withReceiptHandle("test-receipt");

        dlqService.moveMessageToDLQ(message, "test reason");

        verify(amazonSQS).sendMessage(any(SendMessageRequest.class));
        verify(amazonSQS).deleteMessage(QUEUE_URL, message.getReceiptHandle());
    }

    @Test
    void moveMessageToDLQ_WhenSendMessageFails_HandlesException() {
        Message message = new Message()
            .withMessageId("test-message-id")
            .withBody("test-body");

        when(amazonSQS.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(new RuntimeException("Send failed"));

        dlqService.moveMessageToDLQ(message, "test reason");

        verify(amazonSQS).sendMessage(any(SendMessageRequest.class));
        verify(amazonSQS, never()).deleteMessage(anyString(), anyString());
    }
} 