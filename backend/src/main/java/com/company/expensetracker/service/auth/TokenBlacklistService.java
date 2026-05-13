package com.company.expensetracker.service.auth;

import com.company.expensetracker.domain.RevokedTokenJti;
import com.company.expensetracker.repository.RevokedTokenJtiRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Persistent blacklist for revoked JWT refresh-token identifiers (JTI claims).
 *
 * <p>Each revoked JTI is stored in the {@code revoked_token_jtis} table with its
 * original expiry time. A scheduled job ({@link #purgeExpired()}) removes entries
 * whose expiry has passed, keeping the table small.
 *
 * <p>Used by {@link AuthService} to prevent replay attacks on the {@code /refresh}
 * and {@code /logout} endpoints.
 */
@Service
public class TokenBlacklistService {

    private final RevokedTokenJtiRepository repository;

    public TokenBlacklistService(RevokedTokenJtiRepository repository) {
        this.repository = repository;
    }

    /**
     * Adds a JWT JTI to the blacklist.
     *
     * @param jti       the unique JWT identifier to revoke
     * @param expiresAt the token's expiry instant, used for scheduled cleanup
     */
    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        repository.save(new RevokedTokenJti(jti, expiresAt));
    }

    /**
     * Returns {@code true} if the given JTI has been blacklisted.
     *
     * @param jti the JWT identifier to check
     * @return {@code true} if revoked, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return repository.existsById(jti);
    }

    /**
     * Deletes all blacklist entries whose expiry instant is in the past.
     *
     * <p>Runs on a configurable fixed-delay schedule
     * (property {@code app.jwt.blacklist-cleanup-delay}, default 1 hour).
     */
    @Scheduled(fixedDelayString = "${app.jwt.blacklist-cleanup-delay:3600000}")
    @Transactional
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}
