package net.createbrowser.api;

/** Simple token-bucket rate limiter ensuring a minimum interval between requests. */
public final class RateLimiter {

    private final long minIntervalMs;
    private long lastRequestTime = 0;

    public RateLimiter(int maxPerSecond) {
        this.minIntervalMs = 1000L / maxPerSecond;
    }

    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        long wait = minIntervalMs - (now - lastRequestTime);
        if (wait > 0) {
            Thread.sleep(wait);
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
