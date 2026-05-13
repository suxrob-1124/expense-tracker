package com.company.expensetracker.dto.transaction;

import com.company.expensetracker.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Partial-update payload for a transaction (PATCH semantics).
 *
 * <p>All fields are optional. Null values are ignored by
 * {@link TransactionMapper#patchEntity}, so only explicitly provided
 * fields are applied to the persisted entity.
 */
@Schema(description = "Partial-update payload for a transaction. Null fields are ignored.")
public record TransactionPatchRequest(

        @Schema(description = "New amount (must be ≥ 0.01 if provided)", example = "49.99", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,

        @Schema(description = "New transaction type: INCOME or EXPENSE", example = "INCOME", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        TransactionType type,

        @Schema(description = "New description (max 255 characters)", example = "Updated description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255) String description,

        @Schema(description = "New transaction date (UTC ISO-8601)", example = "2026-05-14T08:30:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Instant date,

        @Schema(description = "UUID of the new category (ownership is verified)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID categoryId,

        @Schema(description = "UUID of the payment method to link (ownership is verified). Null leaves the current link unchanged.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID paymentMethodId

) {}
