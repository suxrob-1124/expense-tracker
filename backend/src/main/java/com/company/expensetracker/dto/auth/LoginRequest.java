package com.company.expensetracker.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/login}.
 */
@Schema(description = "Credentials for password-based login.")
public record LoginRequest(
        @Schema(description = "User's email address.", example = "alice@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Email @NotBlank String email,

        @Schema(description = "User's plaintext password (transmitted over HTTPS only).",
                example = "s3cr3tP@ssw0rd!",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password
) {}
