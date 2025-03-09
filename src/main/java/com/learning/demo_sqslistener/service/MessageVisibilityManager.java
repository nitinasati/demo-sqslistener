package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageVisibilityManager {
    private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityManager.class);
    private final AmazonSQS amazonSQS;
    private final String queueUrl;

    public MessageVisibilityManager(AmazonSQS amazonSQS,
                                  @Value("${aws.sqs.url}") String queueUrl) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = queueUrl;
    }

    public void changeVisibility(Message message, int visibilityTimeout) {
        try {
            ChangeMessageVisibilityRequest request = new ChangeMessageVisibilityRequest()
                .withQueueUrl(queueUrl)
                .withReceiptHandle(message.getReceiptHandle())
                .withVisibilityTimeout(visibilityTimeout);
            
            amazonSQS.changeMessageVisibility(request);
            logger.debug("Changed visibility timeout for message {} to {} seconds", 
                message.getMessageId(), visibilityTimeout);
        } catch (Exception e) {
            logger.error("Failed to change message visibility for {}", message.getMessageId(), e);
        }
    }
} 