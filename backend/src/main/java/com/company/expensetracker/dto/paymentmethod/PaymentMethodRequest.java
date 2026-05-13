package com.company.expensetracker.dto.paymentmethod;

import com.company.expensetracker.domain.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating a payment method.
 *
 * <p>{@code last4} is optional and only meaningful for {@code CARD} and {@code BANK} types,
 * but its format is enforced (exactly 4 digits) regardless of {@link PaymentMethodType}.
 */
@Schema(description = "Payload for creating a payment method.")
public record PaymentMethodRequest(
        @Schema(description = "Payment method display name (1–64 chars, unique per user, case-insensitive).",
                example = "Visa Gold",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 1, max = 64) String name,

        @Schema(description = "Payment method type. One of: CARD, CASH, BANK.",
                example = "CARD",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull PaymentMethodType type,

        @Schema(description = "Last four digits of the card or account number (optional, 4 digits).",
                example = "1234",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{4}$") String last4,

        @Schema(description = "Initial balance (optional, scale 4, non-negative).",
                example = "1000.0000",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.0000", inclusive = true) BigDecimal balance
) {}
