package com.ingenium.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class IngeniumExecutorsTest {

    @BeforeEach
    void setUp() {
        IngeniumExecutors.init();
    }

    @AfterEach
    void tearDown() {
        IngeniumExecutors.shutdown();
    }

    @Test
    void testComputePool() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(0);
        
        IngeniumExecutors.submitCompute(() -> {
            result.set(42);
            latch.countDown();
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(42, result.get());
    }

    @Test
    void testIOPool() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(0);
        
        IngeniumExecutors.submitIO(() -> {
            result.set(100);
            latch.countDown();
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(100, result.get());
    }

    @Test
    void testCommitQueue() {
        AtomicInteger counter = new AtomicInteger(0);
        
        IngeniumExecutors.submitCommit(() -> counter.incrementAndGet());
        IngeniumExecutors.submitCommit(() -> counter.addAndGet(10));
        
        assertEquals(2, IngeniumExecutors.commitQueueSize());
        
        int processed = IngeniumExecutors.drainCommitQueue(TimeUnit.MILLISECONDS.toNanos(100));
        
        assertEquals(2, processed);
        assertEquals(11, counter.get());
        assertEquals(0, IngeniumExecutors.commitQueueSize());
    }

    @Test
    void testDrainCommitQueueBudget() {
        for (int i = 0; i < 1000; i++) {
            IngeniumExecutors.submitCommit(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Drain with a very small budget (1ms)
        // Note: System.nanoTime() and Thread.sleep(1) might not be precise enough for exact 1 count,
        // but it should definitely not drain all 1000.
        int processed = IngeniumExecutors.drainCommitQueue(TimeUnit.MILLISECONDS.toNanos(1));
        
        assertTrue(processed < 1000, "Should not have processed all tasks within 1ms budget, processed: " + processed);
        assertTrue(IngeniumExecutors.commitQueueSize() > 0);
    }
}
