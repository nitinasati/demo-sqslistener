package com.learning.demo_sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class TestAwsConfig {

    @Bean
    @Primary
    public AmazonSQS amazonSQS() {
        AmazonSQS mockSqs = Mockito.mock(AmazonSQS.class);
        // Setup default mock behavior
        Mockito.when(mockSqs.receiveMessage(Mockito.any(ReceiveMessageRequest.class)))
            .thenReturn(new ReceiveMessageResult());
        return mockSqs;
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 