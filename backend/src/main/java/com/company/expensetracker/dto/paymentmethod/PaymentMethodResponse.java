package com.company.expensetracker.dto.paymentmethod;

import com.company.expensetracker.domain.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment method data returned by the API.
 */
@Schema(description = "Payment method as returned by the API.")
public record PaymentMethodResponse(
        @Schema(description = "Unique payment method identifier (UUID).",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Display name.", example = "Visa Gold",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Payment method type: CARD, CASH or BANK.",
                example = "CARD",
                requiredMode = Schema.RequiredMode.REQUIRED)
        PaymentMethodType type,

        @Schema(description = "Last four digits (nullable).",
                example = "1234",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String last4,

        @Schema(description = "Current balance (scale 4, nullable).",
                example = "1000.0000",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal balance,

        @Schema(description = "Archive flag.", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean archived,

        @Schema(description = "Creation timestamp — UTC ISO-8601.",
                example = "2026-05-13T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Last update timestamp — UTC ISO-8601.",
                example = "2026-05-13T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt
) {}
