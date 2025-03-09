package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class DeadLetterQueueService {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private final AmazonSQS amazonSQS;
    private final String sourceQueueUrl;
    private final String deadLetterQueueUrl;

    public DeadLetterQueueService(
            AmazonSQS amazonSQS,
            @Value("${aws.sqs.url}") String sourceQueueUrl,
            @Value("${aws.sqs.dlq.url}") String deadLetterQueueUrl) {
        this.amazonSQS = amazonSQS;
        this.sourceQueueUrl = sourceQueueUrl;
        this.deadLetterQueueUrl = deadLetterQueueUrl;
    }

    public void moveMessageToDLQ(Message message, String failureReason) {
        try {
            SendMessageRequest dlqRequest = new SendMessageRequest()
                .withQueueUrl(deadLetterQueueUrl)
                .withMessageBody(message.getBody())
                .withMessageAttributes(Map.of(
                    "OriginalMessageId", new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue(message.getMessageId()),
                    "FailureReason", new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue(failureReason)
                ));

            amazonSQS.sendMessage(dlqRequest);
            amazonSQS.deleteMessage(sourceQueueUrl, message.getReceiptHandle());
            logger.info("Moved message {} to DLQ with reason: {}", message.getMessageId(), failureReason);
        } catch (Exception e) {
            logger.error("Failed to move message {} to DLQ", message.getMessageId(), e);
        }
    }
} 