package com.company.expensetracker.repository;

import com.company.expensetracker.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Returns all categories owned by the given user, ordered by name ascending.
     *
     * @param userId the owner's UUID
     * @return list of categories; may be empty
     */
    List<Category> findAllByUserIdOrderByNameAsc(UUID userId);

    /**
     * Finds a category by its UUID and owner's UUID.
     * Used for ownership verification before mutations.
     *
     * @param id     the category's UUID
     * @param userId the owner's UUID
     * @return an {@link Optional} with the category, or empty if not found or not owned
     */
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks whether a category with the given name (case-insensitive) exists for the user.
     * Used to enforce unique category names per user.
     *
     * @param userId the owner's UUID
     * @param name   the category name to check
     * @return {@code true} if such a category exists
     */
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
