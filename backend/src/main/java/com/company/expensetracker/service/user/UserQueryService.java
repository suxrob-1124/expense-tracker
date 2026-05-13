package com.company.expensetracker.service.user;

import com.company.expensetracker.dto.user.UserResponse;
import com.company.expensetracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Read-side CQRS service for user queries.
 *
 * <p>All operations run in a read-only transaction ({@code @Transactional(readOnly = true)}).
 */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserQueryService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>Requires {@code ROLE_USER}.
     *
     * @param userId the authenticated user's UUID (extracted from the JWT principal)
     * @return a {@link UserResponse} for the user
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the user does not exist
     */
    @PreAuthorize("hasRole('USER')")
    public UserResponse findMe(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Finds a user by their UUID regardless of authentication context.
     *
     * @param userId the user's UUID
     * @return a {@link UserResponse} for the user
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the user does not exist
     */
    public UserResponse findById(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
