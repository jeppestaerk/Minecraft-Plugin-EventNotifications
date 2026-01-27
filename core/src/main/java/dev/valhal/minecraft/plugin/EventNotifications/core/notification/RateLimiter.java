package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {
    private final long maxTokens;
    private final long refillIntervalMs;
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTime;

    public RateLimiter(long maxTokens, long refillIntervalMs) {
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = new AtomicLong(maxTokens);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    public static RateLimiter perMinute(int maxRequests) {
        return new RateLimiter(maxRequests, 60_000L / maxRequests);
    }

    public static RateLimiter perSecond(int maxRequests) {
        return new RateLimiter(maxRequests, 1000L / maxRequests);
    }

    public synchronized boolean tryAcquire() {
        refill();
        long currentTokens = tokens.get();
        if (currentTokens > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();
        long elapsed = now - lastRefill;

        if (elapsed >= refillIntervalMs) {
            long tokensToAdd = elapsed / refillIntervalMs;
            long newTokens = Math.min(maxTokens, tokens.get() + tokensToAdd);
            tokens.set(newTokens);
            lastRefillTime.set(now);
        }
    }

    public long getAvailableTokens() {
        refill();
        return tokens.get();
    }
}
