package com.learning.demo_sqslistener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RetryManagerTest {

    private RetryManager retryManager;

    @BeforeEach
    void setUp() {
        retryManager = new RetryManager();
    }

    @Test
    void shouldRetry_WhenNoAttemptsMade_ReturnsTrue() {
        assertTrue(retryManager.shouldRetry("message-1"));
    }

    @Test
    void shouldRetry_WhenLessThanMaxRetries_ReturnsTrue() {
        String messageId = "message-1";
        retryManager.incrementRetryCount(messageId);
        retryManager.incrementRetryCount(messageId);
        assertTrue(retryManager.shouldRetry(messageId));
    }

    @Test
    void shouldRetry_WhenMaxRetriesReached_ReturnsFalse() {
        String messageId = "message-1";
        for (int i = 0; i < 3; i++) {
            retryManager.incrementRetryCount(messageId);
        }
        assertFalse(retryManager.shouldRetry(messageId));
    }

    @Test
    void getRetryCount_WhenNoAttempts_ReturnsZero() {
        assertEquals(0, retryManager.getRetryCount("message-1"));
    }

    @Test
    void incrementRetryCount_IncreasesCount() {
        String messageId = "message-1";
        retryManager.incrementRetryCount(messageId);
        assertEquals(1, retryManager.getRetryCount(messageId));
    }

    @Test
    void clearRetryCount_RemovesCount() {
        String messageId = "message-1";
        retryManager.incrementRetryCount(messageId);
        retryManager.clearRetryCount(messageId);
        assertEquals(0, retryManager.getRetryCount(messageId));
    }
} 