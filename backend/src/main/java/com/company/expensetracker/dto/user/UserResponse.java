package com.company.expensetracker.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile returned after registration or via {@code GET /api/v1/users/me}.
 */
@Schema(description = "User profile returned by the API.")
public record UserResponse(
        @Schema(description = "Unique user identifier (UUID).",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "User's email address (decrypted from AES-256-GCM storage).",
                example = "alice@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "First name.", example = "Alice",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String firstName,

        @Schema(description = "Last name.", example = "Smith",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String lastName,

        @Schema(description = "Assigned role.", example = "USER",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String role,

        @Schema(description = "Account creation timestamp — UTC ISO-8601.",
                example = "2026-05-13T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt
) {}
