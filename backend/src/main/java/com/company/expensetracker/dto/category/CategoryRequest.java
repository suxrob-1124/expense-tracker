package com.company.expensetracker.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a category.
 */
@Schema(description = "Payload for creating or updating a user-defined category.")
public record CategoryRequest(
        @Schema(description = "Category display name (1–64 chars, unique per user, case-insensitive).",
                example = "Groceries",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 64) String name,

        @Schema(description = "Hex color code for the category badge.",
                example = "#4ade80",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,

        @Schema(description = "Icon identifier (1–32 chars) from the predefined icon set.",
                example = "shopping-cart",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 32) String icon
) {}
