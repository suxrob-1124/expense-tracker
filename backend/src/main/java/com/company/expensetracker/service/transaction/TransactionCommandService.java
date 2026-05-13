package com.company.expensetracker.service.transaction;

import com.company.expensetracker.domain.Transaction;
import com.company.expensetracker.dto.transaction.TransactionPatchRequest;
import com.company.expensetracker.dto.transaction.TransactionRequest;
import com.company.expensetracker.dto.transaction.TransactionResponse;
import com.company.expensetracker.repository.CategoryRepository;
import com.company.expensetracker.repository.PaymentMethodRepository;
import com.company.expensetracker.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Write-side CQRS service for transactions.
 *
 * <p>Handles all mutating operations: create, update, and delete.
 * Every method runs within a transaction ({@code @Transactional}) and
 * requires role {@code USER} ({@code @PreAuthorize}).
 */
@Service
@Transactional
@PreAuthorize("hasRole('USER')")
public class TransactionCommandService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionMapper transactionMapper;

    public TransactionCommandService(TransactionRepository transactionRepository,
                                     CategoryRepository categoryRepository,
                                     PaymentMethodRepository paymentMethodRepository,
                                     TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionMapper = transactionMapper;
    }

    /**
     * Creates a new transaction.
     *
     * <p>Verifies that the category identified by {@code request.categoryId()}
     * belongs to {@code userId} before persisting.
     *
     * @param userId  UUID of the transaction owner
     * @param request transaction payload
     * @return the persisted transaction
     * @throws ResponseStatusException 404 if the category is not found or belongs to another user
     */
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (request.paymentMethodId() != null) {
            paymentMethodRepository.findByIdAndUserId(request.paymentMethodId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found"));
        }

        Transaction transaction = new Transaction(
                request.amount(),
                request.type(),
                request.description(),
                request.date(),
                request.categoryId(),
                request.paymentMethodId(),
                userId
        );

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    /**
     * Partially updates a transaction.
     *
     * <p>Only non-null fields from {@code request} are applied to the entity via
     * {@link TransactionMapper#patchEntity} ({@code NullValuePropertyMappingStrategy.IGNORE}).
     * If {@code categoryId} changes, ownership of the new category is verified.
     * Optimistic locking is enforced through {@code @Version} on the entity.
     *
     * @param id      UUID of the transaction to update
     * @param userId  UUID of the transaction owner
     * @param request fields to update; null fields are ignored
     * @return the updated transaction
     * @throws ResponseStatusException 404 if the transaction or the new category is not found or belongs to another user
     */
    public TransactionResponse update(UUID id, UUID userId, TransactionPatchRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (request.categoryId() != null && !request.categoryId().equals(transaction.getCategoryId())) {
            categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        }

        if (request.paymentMethodId() != null
                && !request.paymentMethodId().equals(transaction.getPaymentMethodId())) {
            paymentMethodRepository.findByIdAndUserId(request.paymentMethodId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found"));
        }

        transactionMapper.patchEntity(transaction, request);

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    /**
     * Deletes a transaction.
     *
     * <p>Verifies that the transaction identified by {@code id} belongs to {@code userId}
     * before deletion.
     *
     * @param id     UUID of the transaction
     * @param userId UUID of the owner
     * @throws ResponseStatusException 404 if the transaction is not found or belongs to another user
     */
    public void delete(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        transactionRepository.delete(transaction);
    }
}
