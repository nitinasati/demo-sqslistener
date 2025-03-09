package com.learning.demo_sqslistener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RetryManager {
    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);
    private final ConcurrentHashMap<String, Integer> retryCount = new ConcurrentHashMap<>();
    private final int maxRetries;

    public RetryManager(@Value("${sqs.retry.max:3}") int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean shouldRetry(String messageId) {
        return retryCount.getOrDefault(messageId, 0) < maxRetries;
    }

    public void incrementRetryCount(String messageId) {
        retryCount.compute(messageId, (key, count) -> count == null ? 1 : count + 1);
        logger.debug("Incremented retry count for message {} to {}", messageId, getRetryCount(messageId) + 1);
    }

    public int getRetryCount(String messageId) {
        return retryCount.getOrDefault(messageId, 0);
    }

    public void clearRetryCount(String messageId) {
        retryCount.remove(messageId);
        logger.debug("Cleared retry count for message {}", messageId);
    }
} 