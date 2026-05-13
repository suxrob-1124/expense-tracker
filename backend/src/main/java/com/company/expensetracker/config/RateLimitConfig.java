package com.company.expensetracker.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Bucket4j rate-limiting configuration backed by an in-memory Caffeine cache.
 *
 * <p>Two independent bucket caches are maintained — one for authentication paths
 * ({@code /api/v1/auth/**}) and one for all other paths:
 * <ul>
 *   <li>Auth buckets: {@code app.ratelimit.auth-rpm} tokens per minute (default 10).</li>
 *   <li>Default buckets: {@code app.ratelimit.default-rpm} tokens per minute (default 60).</li>
 * </ul>
 *
 * <p>Each bucket is keyed by client IP address. Caffeine evicts idle buckets after
 * 15 minutes; the cache holds up to 100,000 entries.
 */
@Configuration
public class RateLimitConfig {

    @Value("${app.ratelimit.default-rpm:60}")
    private int defaultRpm;

    @Value("${app.ratelimit.auth-rpm:10}")
    private int authRpm;

    private final Cache<String, Bucket> defaultBuckets = buildBucketCache();
    private final Cache<String, Bucket> authBuckets = buildBucketCache();

    /**
     * Returns (or creates) the default-rate Bucket4j bucket for the given IP address.
     *
     * @param ip the client IP key
     * @return a {@link Bucket} configured at the default RPM limit
     */
    public Bucket resolveDefaultBucket(String ip) {
        return defaultBuckets.get(ip, k -> newBucket(defaultRpm));
    }

    /**
     * Returns (or creates) the auth-rate Bucket4j bucket for the given IP address.
     *
     * @param ip the client IP key
     * @return a {@link Bucket} configured at the auth RPM limit (stricter)
     */
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
