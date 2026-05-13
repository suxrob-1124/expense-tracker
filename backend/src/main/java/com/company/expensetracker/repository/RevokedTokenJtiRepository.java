package com.company.expensetracker.repository;

import com.company.expensetracker.domain.RevokedTokenJti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

/**
 * Repository for managing revoked JWT JTI (JWT ID) records used by the token blacklist.
 *
 * <p>On logout or token rotation, the refresh token's JTI is stored here so that
 * subsequent refresh attempts using the same token are rejected. Expired entries are
 * periodically pruned via {@link #deleteByExpiresAtBefore(Instant)}.
 */
public interface RevokedTokenJtiRepository extends JpaRepository<RevokedTokenJti, String> {

    /**
     * Deletes all revoked-token entries whose expiry timestamp is before the given instant.
     *
     * <p>Intended to be called on a schedule (e.g. via {@code @Scheduled}) to prevent
     * unbounded growth of the blacklist table.
     *
     * @param now the cutoff instant; entries with {@code expiresAt < now} are removed
     */
    @Modifying
    @Query("DELETE FROM RevokedTokenJti r WHERE r.expiresAt < :now")
    void deleteByExpiresAtBefore(Instant now);
}
