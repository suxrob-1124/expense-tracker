package com.company.expensetracker.repository;

import com.company.expensetracker.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link PaymentMethod} entities.
 *
 * <p>All lookup methods are user-scoped: they accept the owner's UUID
 * so the service layer can return {@code 404 Not Found} for foreign records
 * without leaking their existence.
 */
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /**
     * Returns all payment methods owned by the given user, sorted by name ascending.
     *
     * @param userId the owner's UUID
     * @return list of payment methods; may be empty
     */
    List<PaymentMethod> findAllByUserIdOrderByNameAsc(UUID userId);

    /**
     * Finds a payment method by its UUID and owner's UUID.
     * Used for ownership verification before mutations or look-ups by other services.
     *
     * @param id     the payment method UUID
     * @param userId the owner's UUID
     * @return an {@link Optional} with the payment method, or empty if not found or not owned
     */
    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks whether a payment method with the given name (case-insensitive)
     * already exists for the user. Used to enforce unique names per user.
     *
     * @param userId the owner's UUID
     * @param name   the candidate name
     * @return {@code true} if such a payment method already exists
     */
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
