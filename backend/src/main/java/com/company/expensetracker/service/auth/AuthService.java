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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for JWT issuance, rotation and revocation.
 *
 * <p>Handles the full authentication lifecycle:
 * <ul>
 *   <li>Credential validation via Spring Security's {@code AuthenticationManager}.</li>
 *   <li>Access and refresh token generation via {@link com.company.expensetracker.security.JwtTokenProvider}.</li>
 *   <li>Refresh token rotation: old JTI blacklisted before new tokens are issued.</li>
 *   <li>Publishing {@link com.company.expensetracker.event.UserLoggedInEvent} and
 *       {@link com.company.expensetracker.event.UserLoginFailedEvent} for audit and
 *       brute-force protection listeners.</li>
 * </ul>
 *
 * <p>All methods are {@code @Transactional}; read-only operations use
 * {@code @Transactional(readOnly = true)}.
 */
@Service
@Transactional
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

    /**
     * Authenticates the user and issues a fresh access + refresh token pair.
     *
     * <p>Publishes {@link com.company.expensetracker.event.UserLoggedInEvent} on success
     * and {@link com.company.expensetracker.event.UserLoginFailedEvent} on failure.
     *
     * @param request     the login credentials (email + password)
     * @param httpRequest the HTTP request used to extract the client IP address for auditing
     * @return a {@link LoginTokens} record containing both tokens and their TTLs
     * @throws org.springframework.web.server.ResponseStatusException {@code 401} for invalid credentials,
     *         {@code 423} when the account is locked
     */
    @Transactional(readOnly = true)
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

    /**
     * Validates the refresh token, revokes its JTI, and issues a new token pair.
     *
     * <p>Token rotation: the incoming JTI is blacklisted before new tokens are generated,
     * preventing reuse.
     *
     * @param refreshToken the raw refresh JWT read from the HttpOnly cookie
     * @return a new {@link LoginTokens} record
     * @throws org.springframework.web.server.ResponseStatusException {@code 401} if the token is
     *         null/blank, expired, of the wrong type, or already revoked
     */
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
            // Known limitation: two concurrent requests with the same token can both pass
            // this check before either writes to the blacklist. Acceptable for MVP;
            // full fix requires INSERT ... ON CONFLICT DO NOTHING at the DB level.
            if (tokenBlacklistService.isRevoked(jti)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
            }

            UUID userId = jwtTokenProvider.extractUserId(claims);
            List<String> roles = jwtTokenProvider.extractRoles(claims);
            String role = roles.isEmpty() ? "ROLE_USER" : roles.get(0);

            // Revoke old JTI before issuing new token (rotation)
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

    /**
     * Revokes the given refresh token by blacklisting its JTI.
     *
     * <p>Silently ignores null/blank tokens and expired or otherwise invalid JWTs —
     * the logout flow should always succeed from the client's perspective.
     *
     * @param refreshToken the raw refresh JWT, may be {@code null} or blank
     */
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
