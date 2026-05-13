package com.company.expensetracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "revoked_token_jtis")
public class RevokedTokenJti {

    @Id
    @Column(name = "jti", length = 36, nullable = false, updatable = false)
    private String jti;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected RevokedTokenJti() {}

    public RevokedTokenJti(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public Instant getExpiresAt() { return expiresAt; }
    public Long getVersion() { return version; }
}
