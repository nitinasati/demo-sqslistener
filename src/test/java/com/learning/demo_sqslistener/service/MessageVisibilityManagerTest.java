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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageVisibilityManagerTest {
    
    private MessageVisibilityManager visibilityManager;
    
    @Mock
    private AmazonSQS amazonSQS;
    
    @Mock
    private Message message;
    
    private static final String QUEUE_URL = "queue-url";

    @BeforeEach
    void setUp() {
        visibilityManager = new MessageVisibilityManager(amazonSQS, QUEUE_URL);
    }

    @Test
    void changeVisibility_Success() {
        when(message.getReceiptHandle()).thenReturn("test-receipt");
        
        visibilityManager.changeVisibility(message, 30);
        
        verify(amazonSQS).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }

    @Test
    void changeVisibility_WithZeroTimeout_Success() {
        when(message.getReceiptHandle()).thenReturn("test-receipt");
        
        visibilityManager.changeVisibility(message, 0);
        
        verify(amazonSQS).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }
} 