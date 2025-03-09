package com.learning.demo_sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.learning.demo_sqslistener.service.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class TestConfig {
    
    private static final String TEST_QUEUE_URL = "http://test-queue.com";
    private static final String TEST_DLQ_URL = "http://test-dlq.com";
    private static final String TEST_API_URL = "http://test-api.com";
    
    @Bean
    public AmazonSQS amazonSQS() {
        return Mockito.mock(AmazonSQS.class);
    }

    @Bean
    public RetryManager retryManager() {
        return new RetryManager(3);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MessageProcessor messageProcessor(RestTemplate restTemplate) {
        return new MessageProcessor(TEST_API_URL);
    }

    @Bean
    public DeadLetterQueueService deadLetterQueueService(AmazonSQS amazonSQS) {
        return new DeadLetterQueueService(amazonSQS, TEST_QUEUE_URL, TEST_DLQ_URL);
    }

    @Bean
    public MessageVisibilityManager messageVisibilityManager(AmazonSQS amazonSQS) {
        return new MessageVisibilityManager(amazonSQS, TEST_QUEUE_URL);
    }

    @Bean
    public SQSListener sqsListener(
            AmazonSQS amazonSQS,
            MessageProcessor messageProcessor,
            RetryManager retryManager,
            DeadLetterQueueService deadLetterQueueService,
            MessageVisibilityManager messageVisibilityManager) {
        return new SQSListener(
            amazonSQS,
            TEST_QUEUE_URL,
            TEST_DLQ_URL,
            messageProcessor,
            retryManager,
            deadLetterQueueService,
            messageVisibilityManager
        );
    }
} 