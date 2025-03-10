package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSListenerTest {

    @Mock private AmazonSQS amazonSQS;
    @Mock private MessageProcessor messageProcessor;
    @Mock private RetryManager retryManager;
    @Mock private DeadLetterQueueService dlqService;
    @Mock private MessageVisibilityManager visibilityManager;

    private SQSListener sqsListener;
    private static final String QUEUE_URL = "queue-url";
    private static final String DLQ_URL = "dlq-url";

    @BeforeEach
    void setUp() {
        sqsListener = new SQSListener(
            amazonSQS,
            QUEUE_URL,
            DLQ_URL,
            messageProcessor,
            retryManager,
            dlqService,
            visibilityManager
        );
    }

    @Test
    void pollMessages_WhenNoMessages_DoesNothing() {
        when(amazonSQS.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(new ReceiveMessageResult().withMessages(Collections.emptyList()));

        sqsListener.pollMessages();

        verify(messageProcessor, never()).processMessage(any(Message.class));
    }

    @Test
    void pollMessages_WithSuccessfulProcessing_DeletesMessage() {
        Message message = createTestMessage();
        when(amazonSQS.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(new ReceiveMessageResult().withMessages(Arrays.asList(message)));
        
        sqsListener.pollMessages();

        verify(messageProcessor).processMessage(message);
        verify(amazonSQS).deleteMessage(QUEUE_URL, message.getReceiptHandle());
        verify(retryManager).clearRetryCount(message.getMessageId());
    }

    @Test
    void pollMessages_WhenProcessingFails_HandlesRetry() {
        Message message = createTestMessage();
        when(amazonSQS.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(new ReceiveMessageResult().withMessages(Arrays.asList(message)));
        doThrow(new RuntimeException("Processing failed"))
            .when(messageProcessor).processMessage(any(Message.class));
        when(retryManager.shouldRetry(anyString())).thenReturn(true);

        sqsListener.pollMessages();

        verify(retryManager).incrementRetryCount(message.getMessageId());
        verify(visibilityManager).changeVisibility(eq(message), anyInt());
    }

    @Test
    void pollMessages_WhenMaxRetriesReached_MovesDLQ() {
        Message message = createTestMessage();
        when(amazonSQS.receiveMessage(any(ReceiveMessageRequest.class)))
            .thenReturn(new ReceiveMessageResult().withMessages(Arrays.asList(message)));
        doThrow(new RuntimeException("Processing failed"))
            .when(messageProcessor).processMessage(any(Message.class));
        when(retryManager.shouldRetry(anyString())).thenReturn(false);

        sqsListener.pollMessages();

        verify(dlqService).moveMessageToDLQ(eq(message), anyString());
        verify(retryManager).clearRetryCount(message.getMessageId());
    }

    private Message createTestMessage() {
        return new Message()
            .withMessageId("test-message-id")
            .withReceiptHandle("test-receipt")
            .withBody("{\"test\":\"data\"}");
    }
} 