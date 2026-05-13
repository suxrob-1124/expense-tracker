package com.company.expensetracker.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body returned after a successful login or token refresh.
 *
 * <p>The companion refresh token is delivered via an {@code HttpOnly; Secure; SameSite=Strict}
 * cookie scoped to {@code /api/v1/auth} and is therefore absent from this record.
 */
@Schema(description = "JWT authentication response returned on successful login or token refresh.")
public record AuthResponse(
        @Schema(description = "Short-lived JWT access token (HS256, 15-minute TTL).",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @Schema(description = "Token scheme — always 'Bearer'.", example = "Bearer",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String tokenType,

        @Schema(description = "Access token lifetime in seconds.", example = "900",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long expiresInSeconds
) {}
