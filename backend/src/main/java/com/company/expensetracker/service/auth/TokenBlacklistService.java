package com.company.expensetracker.service.auth;

import com.company.expensetracker.domain.RevokedTokenJti;
import com.company.expensetracker.repository.RevokedTokenJtiRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenBlacklistService {

    private final RevokedTokenJtiRepository repository;

    public TokenBlacklistService(RevokedTokenJtiRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        repository.save(new RevokedTokenJti(jti, expiresAt));
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return repository.existsByJti(jti);
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}
