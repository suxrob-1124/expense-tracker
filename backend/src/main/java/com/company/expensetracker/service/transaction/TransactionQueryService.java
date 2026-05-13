package com.company.expensetracker.service.transaction;

import com.company.expensetracker.domain.TransactionType;
import com.company.expensetracker.dto.common.PagedResponse;
import com.company.expensetracker.dto.transaction.TransactionResponse;
import com.company.expensetracker.dto.transaction.TransactionSummaryResponse;
import com.company.expensetracker.repository.TransactionRepository;
import com.company.expensetracker.repository.TransactionTotalProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Read-side CQRS service for transactions.
 *
 * <p>Handles all read operations. Every method runs within a read-only
 * transaction ({@code @Transactional(readOnly = true)}) and requires
 * role {@code USER} ({@code @PreAuthorize}).
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('USER')")
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public TransactionQueryService(TransactionRepository transactionRepository,
                                   TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    /**
     * Returns all transactions for the user in the specified month.
     *
     * <p>When both {@code month} and {@code year} are provided the query is
     * scoped to that period; otherwise defaults to the current UTC month.
     *
     * @param userId UUID of the user
     * @param month  month number 1–12 (optional)
     * @param year   calendar year (optional; only applied together with {@code month})
     * @return list of transactions sorted by date descending
     */
    public List<TransactionResponse> findAllForUser(UUID userId, Integer month, Integer year) {
        YearMonth period = (month != null && year != null)
                ? YearMonth.of(year, month)
                : YearMonth.now(ZoneOffset.UTC);

        Instant from = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return transactionRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(userId, from, to)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Returns a page of the user's most recent transactions.
     *
     * @param userId UUID of the user
     * @param page   zero-based page number (must be ≥ 0)
     * @param size   page size (must be between 1 and 50)
     * @return page of transactions sorted by date descending
     * @throws IllegalArgumentException if {@code page < 0} or {@code size} is outside 1–50
     */
    public PagedResponse<TransactionResponse> findLatest(UUID userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50");
        }
        return PagedResponse.from(
                transactionRepository
                        .findAllByUserIdOrderByDateDesc(userId, PageRequest.of(page, size))
                        .map(transactionMapper::toResponse)
        );
    }

    /**
     * Returns a single transaction by ID, enforcing ownership.
     *
     * @param id     UUID of the transaction
     * @param userId UUID of the owning user
     * @return the transaction
     * @throws ResponseStatusException 404 if the transaction does not exist or belongs to another user
     */
    public TransactionResponse findByIdForUser(UUID id, UUID userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    /**
     * Computes income, expense, and balance totals for the specified month.
     *
     * <p>Delegates aggregation to the native JPQL query {@code sumByTypeForPeriod}
     * which groups by {@link TransactionType}. All amounts are scaled to 4 decimal places.
     * Defaults to the current UTC month when parameters are omitted.
     *
     * @param userId UUID of the user
     * @param month  month number 1–12 (optional)
     * @param year   calendar year (optional)
     * @return summary with {@code income}, {@code expense}, and {@code balance} (scale 4)
     */
    public TransactionSummaryResponse summarize(UUID userId, Integer month, Integer year) {
        YearMonth period = (month != null && year != null)
                ? YearMonth.of(year, month)
                : YearMonth.now(ZoneOffset.UTC);
        Instant from = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (TransactionTotalProjection row : transactionRepository.sumByTypeForPeriod(userId, from, to)) {
            if (row.getType() == TransactionType.INCOME) income = row.getTotal();
            else if (row.getType() == TransactionType.EXPENSE) expense = row.getTotal();
        }
        income = income.setScale(4, RoundingMode.HALF_UP);
        expense = expense.setScale(4, RoundingMode.HALF_UP);
        return new TransactionSummaryResponse(income, expense, income.subtract(expense));
    }
}
