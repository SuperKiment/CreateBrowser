package net.createbrowser.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void firstAcquireIsImmediate() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(10);
        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 200, "First acquire should be near-instant, took " + elapsed + " ms");
    }

    @Test
    void secondAcquireRespectsInterval() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(10); // 1 req per 100 ms
        limiter.acquire(); // consume the first slot
        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 80,
            "Second acquire should wait ~100 ms, waited only " + elapsed + " ms");
    }

    @Test
    void slowCallerDoesNotWait() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(5); // 200 ms interval
        limiter.acquire();
        Thread.sleep(250); // longer than interval
        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 100,
            "Acquire after natural cooldown should be fast, took " + elapsed + " ms");
    }
}
