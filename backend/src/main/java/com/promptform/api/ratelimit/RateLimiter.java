package com.promptform.api.ratelimit;

public interface RateLimiter {
    boolean allow(String key);
}
