package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.RateLimiter;

import java.util.List;
import java.util.Map;

/**
 * Service class responsible for listening to AWS SQS messages and managing their processing.
 * Implements message polling, retry logic, dead letter queue handling, and rate limiting.
 *
 * @author demo-sqslistener
 * @version 1.0
 */
@Service
@EnableScheduling
public class SQSListener {

    private static final Logger logger = LoggerFactory.getLogger(SQSListener.class);
    /**
     * Maximum number of retry attempts for processing a message
     */
    private static final int MAX_RETRIES = 3;
    /**
     * Maximum number of messages to retrieve in a single poll
     */
    private static final int MAX_MESSAGES_PER_POLL = 10;
    /**
     * Time period in milliseconds between polls
     */
    private static final int RATE_LIMIT_PERIOD_MS = 1000;
    private final AmazonSQS amazonSQS;
    private final String queueUrl;
    private final String dlqUrl;
    private final MessageProcessor messageProcessor;
    private final RetryManager retryManager;
    private final DeadLetterQueueService dlqService;
    private final MessageVisibilityManager visibilityManager;
    private final RateLimiter rateLimiter;

    /**
     * Constructs a new SQSListener with the specified dependencies.
     *
     * @param amazonSQS AWS SQS client
     * @param queueUrl URL of the main SQS queue
     * @param dlqUrl URL of the dead letter queue
     * @param messageProcessor Service for processing messages
     * @param retryManager Service for managing retry attempts
     * @param dlqService Service for handling dead letter queue operations
     * @param visibilityManager Service for managing message visibility timeouts
     */
    public SQSListener(AmazonSQS amazonSQS,
                      @Value("${aws.sqs.url}") String queueUrl,
                      @Value("${aws.sqs.dlq.url}") String dlqUrl,
                      MessageProcessor messageProcessor,
                      RetryManager retryManager,
                      DeadLetterQueueService dlqService,
                      MessageVisibilityManager visibilityManager) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = queueUrl;
        this.dlqUrl = dlqUrl;
        this.messageProcessor = messageProcessor;
        this.retryManager = retryManager;
        this.dlqService = dlqService;
        this.visibilityManager = visibilityManager;
        this.rateLimiter = RateLimiter.create(MAX_MESSAGES_PER_POLL);
        logger.info("SQSListener initialized with queue URL: {}", queueUrl);
    }

    /**
     * Polls for messages from SQS queue at a fixed rate.
     * Implements rate limiting to prevent overwhelming the queue or downstream services.
     */
    @Scheduled(fixedDelay = RATE_LIMIT_PERIOD_MS)
    public void pollMessages() {
        if (!rateLimiter.tryAcquire()) {
            logger.warn("Rate limit exceeded, skipping poll");
            return;
        }
        try {
            logger.debug("Polling for messages from SQS");
            ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(10)
                .withWaitTimeSeconds(20);

            List<Message> messages = amazonSQS.receiveMessage(receiveRequest).getMessages();
            logger.debug("Received {} messages from SQS", messages.size());

            for (Message message : messages) {
                processMessageWithRetry(message);
            }
        } catch (Exception e) {
            logger.error("Error polling messages from SQS", e);
        }
    }

    /**
     * Processes a message with retry logic.
     * If processing fails, the message will be retried up to MAX_RETRIES times
     * before being moved to the dead letter queue.
     *
     * @param message The SQS message to process
     */
    private void processMessageWithRetry(Message message) {
        String messageId = message.getMessageId();
        try {
            logger.info("Processing message: {} (Attempt: {})", messageId, retryManager.getRetryCount(messageId) + 1);
            messageProcessor.processMessage(message);
            
            // Success - delete message and clear retry count
            amazonSQS.deleteMessage(queueUrl, message.getReceiptHandle());
            retryManager.clearRetryCount(messageId);
            logger.info("Successfully processed and deleted message: {}", messageId);
            
        } catch (Exception e) {
            retryManager.incrementRetryCount(messageId);
            logger.warn("Failed to process message: {} (Attempt: {})", 
                messageId, retryManager.getRetryCount(messageId), e);
            
            if (!retryManager.shouldRetry(messageId)) {
                dlqService.moveMessageToDLQ(message, "Exceeded maximum retry attempts");
                retryManager.clearRetryCount(messageId);
            } else {
                visibilityManager.changeVisibility(message, 30);
            }
        }
    }

    private void moveToDeadLetterQueue(Message message) {
        try {
            // Send to DLQ with original message attributes plus error context
            SendMessageRequest dlqRequest = new SendMessageRequest()
                .withQueueUrl(dlqUrl)
                .withMessageBody(message.getBody())
                .withMessageAttributes(Map.of(
                    "OriginalMessageId", new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue(message.getMessageId()),
                    "FailureReason", new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue("Exceeded maximum retry attempts")
                ));

            amazonSQS.sendMessage(dlqRequest);
            // Delete from original queue
            amazonSQS.deleteMessage(queueUrl, message.getReceiptHandle());
            logger.info("Moved message {} to DLQ after {} failed attempts", 
                message.getMessageId(), MAX_RETRIES);
        } catch (Exception e) {
            logger.error("Failed to move message {} to DLQ", message.getMessageId(), e);
        }
    }

    private void changeMessageVisibility(Message message, int visibilityTimeout) {
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