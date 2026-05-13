package com.company.expensetracker.service.user;

import com.company.expensetracker.crypto.EmailHasher;
import com.company.expensetracker.domain.Role;
import com.company.expensetracker.domain.User;
import com.company.expensetracker.dto.user.ChangePasswordRequest;
import com.company.expensetracker.dto.user.RegisterRequest;
import com.company.expensetracker.dto.user.UserResponse;
import com.company.expensetracker.event.PasswordChangedEvent;
import com.company.expensetracker.event.UserRegisteredEvent;
import com.company.expensetracker.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Write-side CQRS service for user account mutations.
 *
 * <p>All operations run in a {@code @Transactional} context.
 * PII fields (email, firstName, lastName) are stored AES-256-GCM encrypted via
 * {@link com.company.expensetracker.crypto.AesGcmStringConverter}. Email lookups
 * always use the SHA-256 hash produced by {@link com.company.expensetracker.crypto.EmailHasher}.
 * Passwords are hashed with BCrypt (strength 12).
 */
@Service
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailHasher emailHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    public UserCommandService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              EmailHasher emailHasher,
                              ApplicationEventPublisher eventPublisher,
                              UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailHasher = emailHasher;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
    }

    /**
     * Registers a new user account.
     *
     * <p>Verifies email uniqueness by hash before persisting. Publishes
     * {@link com.company.expensetracker.event.UserRegisteredEvent} after the entity is saved.
     *
     * @param request registration payload (email, password, firstName, lastName)
     * @return the persisted user as a {@link UserResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 409} if the email is already registered
     */
    public UserResponse register(RegisterRequest request) {
        String emailHash = emailHasher.hash(request.email());

        if (userRepository.existsByEmailHash(emailHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(
                request.email(),
                emailHash,
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER
        );
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());

        User saved = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getEmailHash(), Instant.now()));

        return userMapper.toResponse(saved);
    }

    /**
     * Updates the user's password after verifying the current one.
     *
     * <p>Requires {@code ROLE_USER}. Publishes
     * {@link com.company.expensetracker.event.PasswordChangedEvent} on success.
     *
     * @param userId  the authenticated user's UUID
     * @param request contains currentPassword (for verification) and newPassword
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the user is not found,
     *         {@code 400} if currentPassword does not match
     */
    @PreAuthorize("hasRole('USER')")
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        eventPublisher.publishEvent(new PasswordChangedEvent(userId, Instant.now()));
    }
}
