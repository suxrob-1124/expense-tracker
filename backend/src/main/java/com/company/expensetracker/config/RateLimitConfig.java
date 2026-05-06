package com.company.expensetracker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class RateLimitConfig {

    @Value("${app.ratelimit.default-rpm:60}")
    private int defaultRpm;

    @Value("${app.ratelimit.auth-rpm:10}")
    private int authRpm;

    private final ConcurrentMap<String, Bucket> defaultBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    public Bucket resolveDefaultBucket(String ip) {
        return defaultBuckets.computeIfAbsent(ip, k -> newBucket(defaultRpm));
    }

    public Bucket resolveAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, k -> newBucket(authRpm));
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
