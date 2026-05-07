package com.company.expensetracker.service.transaction;

import com.company.expensetracker.dto.transaction.TransactionResponse;
import com.company.expensetracker.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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

    public TransactionResponse findByIdForUser(UUID id, UUID userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }
}
