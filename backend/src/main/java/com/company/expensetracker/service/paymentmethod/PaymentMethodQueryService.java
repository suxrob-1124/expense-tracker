package com.company.expensetracker.service.paymentmethod;

import com.company.expensetracker.dto.paymentmethod.PaymentMethodResponse;
import com.company.expensetracker.repository.PaymentMethodRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side CQRS service for payment method queries.
 *
 * <p>All operations run in a read-only transaction ({@code @Transactional(readOnly = true)})
 * and require {@code ROLE_USER}.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('USER')")
public class PaymentMethodQueryService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    public PaymentMethodQueryService(PaymentMethodRepository paymentMethodRepository,
                                     PaymentMethodMapper paymentMethodMapper) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentMethodMapper = paymentMethodMapper;
    }

    /**
     * Returns all payment methods owned by the given user, sorted by name ascending.
     *
     * @param userId the owner's UUID
     * @return list of {@link PaymentMethodResponse}; may be empty
     */
    public List<PaymentMethodResponse> findAllByUserId(UUID userId) {
        return paymentMethodRepository.findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(paymentMethodMapper::toResponse)
                .toList();
    }

    /**
     * Finds a payment method by its UUID and owner.
     *
     * @param id     UUID of the payment method
     * @param userId the requesting user's UUID
     * @return the {@link PaymentMethodResponse}
     * @throws ResponseStatusException {@code 404} if not found or owned by another user
     */
    public PaymentMethodResponse findByIdForUser(UUID id, UUID userId) {
        return paymentMethodRepository.findByIdAndUserId(id, userId)
                .map(paymentMethodMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found"));
    }
}
