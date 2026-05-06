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

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, "access", accessTtl);
    }

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

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return (List<String>) claims.get("roles");
    }

    public long getAccessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public long getRefreshTtlSeconds() {
        return refreshTtl.toSeconds();
    }
}
