package com.company.expensetracker.dto.transaction;

import com.company.expensetracker.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        TransactionType type,
        String description,
        Instant date,
        UUID categoryId,
        Instant createdAt,
        Instant updatedAt
) {}
