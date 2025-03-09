package com.learning.demo_sqslistener.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SQSService {
    
    private final AmazonSQS amazonSQS;
    private final String queueUrl;

    public SQSService(AmazonSQS amazonSQS, @Value("${aws.sqs.url}") String queueUrl) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = queueUrl;
    }

    public SendMessageResult sendMessage(String message) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(message);
        return amazonSQS.sendMessage(sendMessageRequest);
    }
} 