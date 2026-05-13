package com.company.expensetracker.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/v1/users/me/password}.
 */
@Schema(description = "Payload for changing the authenticated user's password.")
public record ChangePasswordRequest(
        @Schema(description = "The user's current password, required for verification.",
                example = "OldP@ssw0rd!",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String currentPassword,

        @Schema(description = "New password (12–128 chars).",
                example = "N3wP@ssw0rd!XYZ",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 12, max = 128) String newPassword
) {}
