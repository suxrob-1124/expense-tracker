package com.company.expensetracker.repository;

import com.company.expensetracker.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUserIdAndDateBetweenOrderByDateDesc(UUID userId, Instant from, Instant to);

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    long countByCategoryIdAndUserId(UUID categoryId, UUID userId);
}
