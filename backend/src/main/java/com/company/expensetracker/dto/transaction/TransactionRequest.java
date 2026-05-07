package com.company.expensetracker.dto.transaction;

import com.company.expensetracker.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @NotNull TransactionType type,
        @Size(max = 255) String description,
        @NotNull Instant date,
        @NotNull UUID categoryId
) {}
