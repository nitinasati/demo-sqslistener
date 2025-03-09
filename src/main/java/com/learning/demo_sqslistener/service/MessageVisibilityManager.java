package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.Message;
import com.learning.demo_sqslistener.exception.ErrorCodes;
import com.learning.demo_sqslistener.exception.SQSProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageVisibilityManager {
    private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityManager.class);
    private final AmazonSQS amazonSQS;
    private final String queueUrl;

    public MessageVisibilityManager(
            AmazonSQS amazonSQS,
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
            String errorMessage = String.format("Message ID: %s, Timeout: %d seconds", 
                message.getMessageId(), visibilityTimeout);
            throw new SQSProcessingException(ErrorCodes.SQS_VISIBILITY_UPDATE_ERROR, errorMessage, e);
        }
    }
} 