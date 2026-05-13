package com.company.expensetracker.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Category data returned by the API.
 */
@Schema(description = "Category as returned by the API.")
public record CategoryResponse(
        @Schema(description = "Unique category identifier (UUID).",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Category display name.", example = "Groceries",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Hex color code.", example = "#4ade80",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String color,

        @Schema(description = "Icon identifier.", example = "shopping-cart",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String icon,

        @Schema(description = "Category creation timestamp — UTC ISO-8601.",
                example = "2026-05-13T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Last update timestamp — UTC ISO-8601.",
                example = "2026-05-13T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt
) {}
