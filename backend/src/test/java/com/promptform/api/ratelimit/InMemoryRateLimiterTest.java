package com.promptform.api.ratelimit;

import com.promptform.api.config.RateLimitProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {
    @Test
    void rejectsRequestsAfterTheConfiguredWindowQuota() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new RateLimitProperties(true, 2));

        assertThat(limiter.allow("generation:test")).isTrue();
        assertThat(limiter.allow("generation:test")).isTrue();
        assertThat(limiter.allow("generation:test")).isFalse();
        assertThat(limiter.allow("generation:other")).isTrue();
    }
}
