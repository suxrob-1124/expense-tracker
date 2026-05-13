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

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUserIdAndDateBetweenOrderByDateDesc(UUID userId, Instant from, Instant to);

    Page<Transaction> findAllByUserIdOrderByDateDesc(UUID userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    long countByCategoryIdAndUserId(UUID categoryId, UUID userId);

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
