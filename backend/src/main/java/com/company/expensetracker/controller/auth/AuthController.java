package com.company.expensetracker.controller.auth;

import com.company.expensetracker.dto.auth.AuthResponse;
import com.company.expensetracker.dto.auth.LoginRequest;
import com.company.expensetracker.service.auth.AuthService;
import com.company.expensetracker.service.auth.LoginTokens;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for JWT-based authentication.
 *
 * <p>Base path: {@code /api/v1/auth}. The {@code /login} and {@code /refresh} endpoints
 * are publicly accessible. {@code /logout} requires a valid access token.
 * The refresh token is issued and stored in an {@code HttpOnly; Secure; SameSite=Strict}
 * cookie scoped to {@code /api/v1/auth}.
 */
@Tag(name = "Authentication", description = "JWT-based login, token refresh and logout.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user with email and password.
     *
     * <p>On success, sets a {@code refreshToken} HttpOnly cookie and returns
     * an access token in the response body.
     *
     * @param request      login credentials (email + password)
     * @param httpRequest  incoming servlet request used to extract the client IP for auditing
     * @param httpResponse servlet response used to set the refresh token cookie
     * @return {@code 200 OK} with {@link AuthResponse} containing the access token
     * @throws org.springframework.web.server.ResponseStatusException {@code 401} if credentials are invalid,
     *         {@code 423} if the account is temporarily locked, {@code 429} if the rate limit is exceeded
     */
    @Operation(summary = "Authenticate user",
            description = "Validates credentials and returns an access token. A refresh token is set as an HttpOnly cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid email or blank password"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account temporarily locked after too many failed attempts"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded (10 RPM per IP on /auth/**)"),
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginTokens tokens = authService.login(request, httpRequest);
        setRefreshCookie(httpResponse, tokens.refreshToken(), (int) tokens.refreshTtlSeconds());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), "Bearer", tokens.accessTtlSeconds()));
    }

    /**
     * Issues a new access token using the refresh token cookie.
     *
     * <p>The old refresh token JTI is revoked (rotation) before a new pair is issued.
     *
     * @param httpRequest  incoming servlet request; the refresh token cookie is read from here
     * @param httpResponse servlet response; the new refresh token cookie is set here
     * @return {@code 200 OK} with a new {@link AuthResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 401} if the refresh token
     *         is missing, expired, revoked, or of the wrong type
     */
    @Operation(summary = "Refresh access token",
            description = "Rotates the refresh token: revokes the old JTI and issues a new access + refresh token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Refresh token missing, expired or revoked"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshCookie(httpRequest);
        LoginTokens tokens = authService.refresh(refreshToken);
        setRefreshCookie(httpResponse, tokens.refreshToken(), (int) tokens.refreshTtlSeconds());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken(), "Bearer", tokens.accessTtlSeconds()));
    }

    /**
     * Revokes the current refresh token and clears the cookie.
     *
     * <p>Idempotent: if the refresh token is already invalid the endpoint still responds with {@code 204}.
     *
     * @param httpRequest  incoming servlet request; the refresh token cookie is read from here
     * @param httpResponse servlet response; the refresh token cookie is cleared here
     * @return {@code 204 No Content}
     */
    @Operation(summary = "Logout",
            description = "Revokes the refresh token JTI and clears the refresh token cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Access token missing or invalid"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshCookie(httpRequest);
        authService.revokeRefreshToken(refreshToken);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
