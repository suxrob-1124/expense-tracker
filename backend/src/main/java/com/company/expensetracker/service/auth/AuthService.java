package com.company.expensetracker.service.auth;

import com.company.expensetracker.crypto.EmailHasher;
import com.company.expensetracker.dto.auth.LoginRequest;
import com.company.expensetracker.event.UserLoggedInEvent;
import com.company.expensetracker.event.UserLoginFailedEvent;
import com.company.expensetracker.security.JwtTokenProvider;
import com.company.expensetracker.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailHasher emailHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       EmailHasher emailHasher,
                       ApplicationEventPublisher eventPublisher,
                       TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailHasher = emailHasher;
        this.eventPublisher = eventPublisher;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public LoginTokens login(LoginRequest request, HttpServletRequest httpRequest) {
        String emailHash = emailHasher.hash(request.email());
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(principal);
            String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

            String ipAddress = extractIpAddress(httpRequest);
            eventPublisher.publishEvent(new UserLoggedInEvent(principal.userId(), Instant.now(), ipAddress));

            return new LoginTokens(
                    accessToken,
                    refreshToken,
                    jwtTokenProvider.getAccessTtlSeconds(),
                    jwtTokenProvider.getRefreshTtlSeconds(),
                    principal.userId()
            );
        } catch (LockedException e) {
            eventPublisher.publishEvent(new UserLoginFailedEvent(emailHash, Instant.now(), "ACCOUNT_LOCKED"));
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is temporarily locked");
        } catch (BadCredentialsException | DisabledException e) {
            eventPublisher.publishEvent(new UserLoginFailedEvent(emailHash, Instant.now(), "BAD_CREDENTIALS"));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    public LoginTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing");
        }
        try {
            Jws<Claims> jws = jwtTokenProvider.parseAndValidate(refreshToken);
            Claims claims = jws.getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token type");
            }
            String jti = claims.getId();
            if (tokenBlacklistService.isRevoked(jti)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
            }

            UUID userId = jwtTokenProvider.extractUserId(claims);
            List<String> roles = jwtTokenProvider.extractRoles(claims);
            String role = roles.isEmpty() ? "ROLE_USER" : roles.get(0);

            // Revoke the old token before issuing a new one (rotation)
            Date expiration = claims.getExpiration();
            tokenBlacklistService.revoke(jti, expiration.toInstant());

            UserPrincipal principal = new UserPrincipal(userId, null, null, role, true, null);
            String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

            return new LoginTokens(
                    newAccessToken,
                    newRefreshToken,
                    jwtTokenProvider.getAccessTtlSeconds(),
                    jwtTokenProvider.getRefreshTtlSeconds(),
                    userId
            );
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        try {
            Jws<Claims> jws = jwtTokenProvider.parseAndValidate(refreshToken);
            Claims claims = jws.getPayload();
            if ("refresh".equals(claims.get("type", String.class))) {
                tokenBlacklistService.revoke(claims.getId(), claims.getExpiration().toInstant());
            }
        } catch (JwtException ignored) {
            // Expired or invalid token — nothing to revoke
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
