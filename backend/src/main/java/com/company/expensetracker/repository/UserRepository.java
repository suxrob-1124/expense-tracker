package com.company.expensetracker.repository;

import com.company.expensetracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by the SHA-256 hash of their (lowercased, trimmed) email address.
     *
     * <p>Never query by the {@code email_encrypted} column — always use this method
     * via {@link com.company.expensetracker.crypto.EmailHasher}.
     *
     * @param emailHash the hex-encoded SHA-256 hash of the normalised email
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByEmailHash(String emailHash);

    /**
     * Checks whether a user with the given email hash already exists.
     *
     * @param emailHash the hex-encoded SHA-256 hash of the normalised email
     * @return {@code true} if a user with that hash exists
     */
    boolean existsByEmailHash(String emailHash);
}
