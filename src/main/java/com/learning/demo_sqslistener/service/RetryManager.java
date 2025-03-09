package com.learning.demo_sqslistener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RetryManager {
    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);
    private static final int MAX_RETRIES = 3;
    private final Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    public boolean shouldRetry(String messageId) {
        int attempts = getRetryCount(messageId);
        return attempts < MAX_RETRIES;
    }

    public int getRetryCount(String messageId) {
        return retryCount.getOrDefault(messageId, 0);
    }

    public void incrementRetryCount(String messageId) {
        int current = getRetryCount(messageId);
        retryCount.put(messageId, current + 1);
        logger.debug("Incremented retry count for message {} to {}", messageId, current + 1);
    }

    public void clearRetryCount(String messageId) {
        retryCount.remove(messageId);
        logger.debug("Cleared retry count for message {}", messageId);
    }
} 