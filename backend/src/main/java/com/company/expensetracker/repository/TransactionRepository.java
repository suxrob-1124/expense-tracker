package com.company.expensetracker.repository;

import com.company.expensetracker.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link Transaction} entities.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Returns all transactions for the user within {@code [from, to)}, newest first. */
    List<Transaction> findAllByUserIdAndDateBetweenOrderByDateDesc(UUID userId, Instant from, Instant to);

    /** Returns a page of all transactions for the user, newest first. */
    Page<Transaction> findAllByUserIdOrderByDateDesc(UUID userId, Pageable pageable);

    /** Returns the transaction only if it belongs to the given user. */
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Counts how many transactions reference the given category for the user.
     * Used by the category service to block deletion of non-empty categories.
     */
    long countByCategoryIdAndUserId(UUID categoryId, UUID userId);

    /**
     * Aggregates transaction totals grouped by {@link com.company.expensetracker.domain.TransactionType}
     * for the given user and time window {@code [from, to)}.
     *
     * @param userId UUID of the user
     * @param from   start of period (inclusive)
     * @param to     end of period (exclusive)
     * @return one projection row per transaction type that has at least one entry
     */
    @Query("""
           SELECT t.type AS type, COALESCE(SUM(t.amount), 0) AS total
           FROM Transaction t
           WHERE t.userId = :userId AND t.date >= :from AND t.date < :to
           GROUP BY t.type
           """)
    List<TransactionTotalProjection> sumByTypeForPeriod(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
