package com.company.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Spring component responsible for issuing and validating HMAC-SHA-256 (HS256) JWTs.
 *
 * <p>Access tokens contain the {@code sub} (userId), {@code roles}, and {@code type=access} claims.
 * Refresh tokens carry the same claims with {@code type=refresh} and a longer TTL.
 * Token lifetimes are configurable via {@code app.jwt.access-ttl} and
 * {@code app.jwt.refresh-ttl} properties.
 */
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.secret}")
    private String secretBase64;

    @Value("${app.jwt.access-ttl}")
    private Duration accessTtl;

    @Value("${app.jwt.refresh-ttl}")
    private Duration refreshTtl;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
    }

    /**
     * Generates a signed access token for the given principal.
     *
     * @param principal the authenticated user
     * @return compact signed JWT access token string
     */
    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, "access", accessTtl);
    }

    /**
     * Generates a signed refresh token for the given principal.
     *
     * @param principal the authenticated user
     * @return compact signed JWT refresh token string
     */
    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, "refresh", refreshTtl);
    }

    private String buildToken(UserPrincipal principal, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(principal.userId().toString())
                .claim("roles", List.of(principal.role()))
                .claim("type", type)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT string against the application signing key.
     *
     * @param token the raw JWT string
     * @return the parsed and verified {@link Jws} containing the claims
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or has an invalid signature
     */
    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * Extracts the user UUID from the {@code sub} claim.
     *
     * @param claims parsed JWT claims
     * @return the user's UUID
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the list of role strings from the {@code roles} claim.
     *
     * @param claims parsed JWT claims
     * @return list of role strings (e.g. {@code ["ROLE_USER"]})
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return (List<String>) claims.get("roles");
    }

    /**
     * Returns the access token TTL in seconds.
     * Used by the controller to populate the {@link com.company.expensetracker.dto.auth.AuthResponse}.
     *
     * @return access token lifetime in seconds
     */
    public long getAccessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /**
     * Returns the refresh token TTL in seconds.
     * Used by the controller to set the refresh token cookie {@code max-age}.
     *
     * @return refresh token lifetime in seconds
     */
    public long getRefreshTtlSeconds() {
        return refreshTtl.toSeconds();
    }
}
