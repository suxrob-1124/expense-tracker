package com.company.expensetracker.repository;

import com.company.expensetracker.domain.TransactionType;

import java.math.BigDecimal;

public interface TransactionTotalProjection {
    TransactionType getType();
    BigDecimal getTotal();
}
