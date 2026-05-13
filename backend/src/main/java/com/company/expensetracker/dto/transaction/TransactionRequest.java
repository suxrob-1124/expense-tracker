package com.company.expensetracker.dto.transaction;

import com.company.expensetracker.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for creating a transaction.
 *
 * <p>All monetary amounts use {@link BigDecimal} with scale 4 internally,
 * but the input value must be ≥ 0.01.
 */
@Schema(description = "Payload for creating a new transaction")
public record TransactionRequest(

        @Schema(description = "Transaction amount (must be ≥ 0.01, stored at scale 4)", example = "99.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,

        @Schema(description = "Transaction type: INCOME or EXPENSE", example = "EXPENSE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull TransactionType type,

        @Schema(description = "Optional description (max 255 characters)", example = "Grocery shopping", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255) String description,

        @Schema(description = "Transaction date and time in ISO-8601 UTC format", example = "2026-05-13T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Instant date,

        @Schema(description = "UUID of the category that owns this transaction", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID categoryId

) {}
