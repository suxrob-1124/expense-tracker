package com.company.expensetracker.dto.transaction;

import com.company.expensetracker.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing a persisted transaction.
 *
 * <p>All monetary values are {@link BigDecimal} with scale 4.
 * Timestamps are UTC {@link Instant} values serialised as ISO-8601 strings.
 */
@Schema(description = "Persisted transaction returned by the API")
public record TransactionResponse(

        @Schema(description = "Unique identifier of the transaction", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Transaction amount (scale 4)", example = "99.9900")
        BigDecimal amount,

        @Schema(description = "Transaction type: INCOME or EXPENSE", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Optional description", example = "Grocery shopping")
        String description,

        @Schema(description = "Transaction date and time (UTC ISO-8601)", example = "2026-05-13T10:00:00Z")
        Instant date,

        @Schema(description = "UUID of the associated category", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "UUID of the linked payment method, or null if none",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID paymentMethodId,

        @Schema(description = "Record creation timestamp (UTC ISO-8601)", example = "2026-05-13T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Record last-update timestamp (UTC ISO-8601)", example = "2026-05-13T10:00:00Z")
        Instant updatedAt

) {}
