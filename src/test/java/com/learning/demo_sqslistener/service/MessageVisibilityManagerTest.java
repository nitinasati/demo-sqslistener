package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageVisibilityManagerTest {

    @Mock
    private AmazonSQS amazonSQS;

    private MessageVisibilityManager visibilityManager;
    private static final String QUEUE_URL = "queue-url";

    @BeforeEach
    void setUp() {
        visibilityManager = new MessageVisibilityManager(amazonSQS, QUEUE_URL);
    }

    @Test
    void changeVisibility_SuccessfulChange() {
        Message message = new Message()
            .withMessageId("test-message-id")
            .withReceiptHandle("test-receipt");

        visibilityManager.changeVisibility(message, 30);

        verify(amazonSQS).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }

    @Test
    void changeVisibility_WhenChangeFails_HandlesException() {
        Message message = new Message()
            .withMessageId("test-message-id")
            .withReceiptHandle("test-receipt");

        doThrow(new RuntimeException("Change failed"))
            .when(amazonSQS).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));

        visibilityManager.changeVisibility(message, 30);

        verify(amazonSQS).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }
} 