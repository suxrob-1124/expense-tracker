package com.company.expensetracker.service.transaction;

import com.company.expensetracker.domain.Transaction;
import com.company.expensetracker.dto.transaction.TransactionPatchRequest;
import com.company.expensetracker.dto.transaction.TransactionRequest;
import com.company.expensetracker.dto.transaction.TransactionResponse;
import com.company.expensetracker.repository.CategoryRepository;
import com.company.expensetracker.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
@PreAuthorize("hasRole('USER')")
public class TransactionCommandService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    public TransactionCommandService(TransactionRepository transactionRepository,
                                     CategoryRepository categoryRepository,
                                     TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionMapper = transactionMapper;
    }

    public TransactionResponse create(UUID userId, TransactionRequest request) {
        categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Transaction transaction = new Transaction(
                request.amount(),
                request.type(),
                request.description(),
                request.date(),
                request.categoryId(),
                userId
        );

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    public TransactionResponse update(UUID id, UUID userId, TransactionPatchRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (request.categoryId() != null && !request.categoryId().equals(transaction.getCategoryId())) {
            categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        }

        transactionMapper.patchEntity(transaction, request);

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    public void delete(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        transactionRepository.delete(transaction);
    }
}
