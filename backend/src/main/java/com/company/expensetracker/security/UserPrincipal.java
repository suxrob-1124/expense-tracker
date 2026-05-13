package com.company.expensetracker.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Immutable principal record returned by {@link CustomUserDetailsService}.
 *
 * <p>Implements Spring Security's {@link UserDetails}. Wraps the user's UUID,
 * email hash, BCrypt password hash, role, enabled flag and optional lock expiry.
 *
 * @param userId       the user's UUID (used as the security principal identifier)
 * @param emailHash    SHA-256 hash of the normalised email (stored instead of plain email)
 * @param passwordHash BCrypt-hashed password (strength 12)
 * @param role         granted role string, e.g. {@code "ROLE_USER"}
 * @param enabled      whether the account is active
 * @param lockedUntil  if non-null, the account is locked until this instant
 */
public record UserPrincipal(
        UUID userId,
        String emailHash,
        String passwordHash,
        String role,
        boolean enabled,
        Instant lockedUntil
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(Instant.now());
    }
}
