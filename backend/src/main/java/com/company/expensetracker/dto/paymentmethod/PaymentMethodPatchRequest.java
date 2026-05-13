package com.company.expensetracker.dto.paymentmethod;

import com.company.expensetracker.domain.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Partial-update payload for a payment method (PATCH semantics).
 *
 * <p>All fields are optional. Null values are ignored by the MapStruct mapper
 * ({@code NullValuePropertyMappingStrategy.IGNORE}), so only explicitly provided
 * fields are applied to the persisted entity. The {@code archived} flag is also
 * exposed here so callers can archive or unarchive without a dedicated endpoint.
 */
@Schema(description = "Partial-update payload for a payment method. Null fields are ignored.")
public record PaymentMethodPatchRequest(
        @Schema(description = "New display name (1–64 chars).",
                example = "Visa Platinum",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(min = 1, max = 64) String name,

        @Schema(description = "New payment method type: CARD, CASH or BANK.",
                example = "CARD",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentMethodType type,

        @Schema(description = "New last4 digits (exactly 4 digits, or null to leave unchanged).",
                example = "5678",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^\\d{4}$") String last4,

        @Schema(description = "New balance (scale 4, non-negative).",
                example = "1500.0000",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.0000", inclusive = true) BigDecimal balance,

        @Schema(description = "Archive flag — true hides the method from default lists, false restores it.",
                example = "true",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean archived
) {}
