package com.learning.demo_sqslistener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetryManagerTest {
    private RetryManager retryManager;
    private static final String MESSAGE_ID = "test-message-id";
    private static final int MAX_RETRIES = 3;

    @BeforeEach
    void setUp() {
        retryManager = new RetryManager(MAX_RETRIES);
    }

    @Test
    void shouldRetry_WhenBelowMaxRetries_ReturnsTrue() {
        retryManager.incrementRetryCount(MESSAGE_ID);
        assertTrue(retryManager.shouldRetry(MESSAGE_ID));
    }

    @Test
    void shouldRetry_WhenMaxRetriesReached_ReturnsFalse() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            retryManager.incrementRetryCount(MESSAGE_ID);
        }
        assertFalse(retryManager.shouldRetry(MESSAGE_ID));
    }

    @Test
    void getRetryCount_WhenNoRetries_ReturnsZero() {
        assertEquals(0, retryManager.getRetryCount(MESSAGE_ID));
    }

    @Test
    void clearRetryCount_RemovesMessageFromTracking() {
        retryManager.incrementRetryCount(MESSAGE_ID);
        retryManager.clearRetryCount(MESSAGE_ID);
        assertEquals(0, retryManager.getRetryCount(MESSAGE_ID));
    }
} 