package com.company.expensetracker.dto.transaction;

import com.company.expensetracker.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionPatchRequest(
        @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        TransactionType type,
        @Size(max = 255) String description,
        Instant date,
        UUID categoryId
) {}
