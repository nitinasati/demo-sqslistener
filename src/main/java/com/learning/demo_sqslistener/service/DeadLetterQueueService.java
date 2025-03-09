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
    private final String dlqUrl;
    private final String queueUrl;

    public DeadLetterQueueService(AmazonSQS amazonSQS,
                                 @Value("${aws.sqs.dlq.url}") String dlqUrl,
                                 @Value("${aws.sqs.url}") String queueUrl) {
        this.amazonSQS = amazonSQS;
        this.dlqUrl = dlqUrl;
        this.queueUrl = queueUrl;
    }

    public void moveMessageToDLQ(Message message, String reason) {
        try {
            SendMessageRequest dlqRequest = new SendMessageRequest()
                .withQueueUrl(dlqUrl)
                .withMessageBody(message.getBody())
                .withMessageAttributes(Map.of(
                    "OriginalMessageId", new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue(message.getMessageId()),
                    "FailureReason", new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue(reason)
                ));

            amazonSQS.sendMessage(dlqRequest);
            amazonSQS.deleteMessage(queueUrl, message.getReceiptHandle());
            logger.info("Moved message {} to DLQ. Reason: {}", message.getMessageId(), reason);
        } catch (Exception e) {
            logger.error("Failed to move message {} to DLQ", message.getMessageId(), e);
        }
    }
} 