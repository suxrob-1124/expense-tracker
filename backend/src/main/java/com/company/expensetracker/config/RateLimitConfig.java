package com.company.expensetracker.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${app.ratelimit.default-rpm:60}")
    private int defaultRpm;

    @Value("${app.ratelimit.auth-rpm:10}")
    private int authRpm;

    private final Cache<String, Bucket> defaultBuckets = buildBucketCache();
    private final Cache<String, Bucket> authBuckets = buildBucketCache();

    public Bucket resolveDefaultBucket(String ip) {
        return defaultBuckets.get(ip, k -> newBucket(defaultRpm));
    }

    public Bucket resolveAuthBucket(String ip) {
        return authBuckets.get(ip, k -> newBucket(authRpm));
    }

    private static Cache<String, Bucket> buildBucketCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .maximumSize(100_000)
                .build();
    }

    private Bucket newBucket(int rpm) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rpm)
                .refillGreedy(rpm, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
