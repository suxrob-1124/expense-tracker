package com.company.expensetracker.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/v1/users/register}.
 */
@Schema(description = "Payload for creating a new user account.")
public record RegisterRequest(
        @Schema(description = "User's email address. Stored AES-256-GCM encrypted; lookup uses SHA-256 hash.",
                example = "alice@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Email @NotBlank String email,

        @Schema(description = "Password (12–128 chars). Stored as BCrypt hash (strength 12) on the server.",
                example = "Sup3rS3cr3t!Pass",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 12, max = 128) String password,

        @Schema(description = "First name (max 100 chars).", example = "Alice",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100) String firstName,

        @Schema(description = "Last name (max 100 chars).", example = "Smith",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100) String lastName
) {}
