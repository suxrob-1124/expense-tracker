package com.company.expensetracker.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Aggregated income/expense summary for a calendar month.
 *
 * <p>All values are {@link BigDecimal} with scale 4.
 * {@code balance} equals {@code income} minus {@code expense}.
 */
@Schema(description = "Monthly income/expense summary")
public record TransactionSummaryResponse(

        @Schema(description = "Total income for the period (scale 4)", example = "1500.0000")
        BigDecimal income,

        @Schema(description = "Total expenses for the period (scale 4)", example = "800.0000")
        BigDecimal expense,

        @Schema(description = "Balance: income minus expense (scale 4)", example = "700.0000")
        BigDecimal balance

) {}
