package com.company.expensetracker.service.paymentmethod;

import com.company.expensetracker.domain.PaymentMethod;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodPatchRequest;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodRequest;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodResponse;
import com.company.expensetracker.repository.PaymentMethodRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Write-side CQRS service for payment method mutations.
 *
 * <p>All operations are {@code @Transactional} and require {@code ROLE_USER}.
 * Every operation verifies that the target payment method belongs to the requesting user
 * via {@link PaymentMethodRepository#findByIdAndUserId}.
 */
@Service
@Transactional
@PreAuthorize("hasRole('USER')")
public class PaymentMethodCommandService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    public PaymentMethodCommandService(PaymentMethodRepository paymentMethodRepository,
                                       PaymentMethodMapper paymentMethodMapper) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentMethodMapper = paymentMethodMapper;
    }

    /**
     * Creates a new payment method owned by the given user.
     *
     * @param userId  the owner's UUID
     * @param request payment method payload (name, type, optional last4, optional balance)
     * @return the persisted payment method as a {@link PaymentMethodResponse}
     * @throws ResponseStatusException {@code 409} if a payment method with the same name already exists for this user
     */
    public PaymentMethodResponse create(UUID userId, PaymentMethodRequest request) {
        if (paymentMethodRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment method name already exists");
        }

        PaymentMethod paymentMethod = new PaymentMethod(
                request.name(),
                request.type(),
                request.last4(),
                request.balance(),
                userId
        );

        return paymentMethodMapper.toResponse(paymentMethodRepository.save(paymentMethod));
    }

    /**
     * Partially updates a payment method.
     *
     * <p>Only non-null fields from {@code request} are applied. If {@code name}
     * is provided and differs from the current value (case-insensitive),
     * uniqueness is re-checked. Optimistic locking is enforced via {@code @Version}.
     *
     * @param id      UUID of the payment method to update
     * @param userId  the requesting user's UUID
     * @param request fields to update; null fields are ignored
     * @return the updated {@link PaymentMethodResponse}
     * @throws ResponseStatusException {@code 404} if the payment method is not found or owned by another user,
     *                                 {@code 409} if the new name conflicts with an existing payment method
     */
    public PaymentMethodResponse update(UUID id, UUID userId, PaymentMethodPatchRequest request) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found"));

        if (request.name() != null
                && !paymentMethod.getName().equalsIgnoreCase(request.name())
                && paymentMethodRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment method name already exists");
        }

        paymentMethodMapper.patchEntity(paymentMethod, request);

        return paymentMethodMapper.toResponse(paymentMethodRepository.save(paymentMethod));
    }

    /**
     * Deletes a payment method after verifying ownership.
     *
     * <p>Transactions that reference this payment method are not deleted —
     * their {@code payment_method_id} is set to {@code NULL} by the database
     * FK ({@code ON DELETE SET NULL}).
     *
     * @param id     UUID of the payment method to delete
     * @param userId the requesting user's UUID
     * @throws ResponseStatusException {@code 404} if the payment method is not found or owned by another user
     */
    public void delete(UUID id, UUID userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found"));

        paymentMethodRepository.delete(paymentMethod);
    }
}
