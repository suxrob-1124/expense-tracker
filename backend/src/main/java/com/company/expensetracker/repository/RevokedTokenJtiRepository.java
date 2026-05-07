package com.company.expensetracker.repository;

import com.company.expensetracker.domain.RevokedTokenJti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface RevokedTokenJtiRepository extends JpaRepository<RevokedTokenJti, String> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM RevokedTokenJti r WHERE r.expiresAt < :now")
    void deleteByExpiresAtBefore(Instant now);
}
