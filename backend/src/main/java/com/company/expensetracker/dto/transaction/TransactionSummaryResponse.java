package com.company.expensetracker.dto.transaction;

import java.math.BigDecimal;

public record TransactionSummaryResponse(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance
) {}
