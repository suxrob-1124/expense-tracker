package com.company.expensetracker.service.category;

import com.company.expensetracker.dto.category.CategoryResponse;
import com.company.expensetracker.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side CQRS service for category queries.
 *
 * <p>All operations run in a read-only transaction ({@code @Transactional(readOnly = true)})
 * and require {@code ROLE_USER}.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('USER')")
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryQueryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /**
     * Returns all categories owned by the given user, sorted by name ascending.
     *
     * @param userId the owner's UUID
     * @return list of {@link CategoryResponse}; may be empty
     */
    public List<CategoryResponse> findAllByUserId(UUID userId) {
        return categoryRepository.findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Finds a category by its UUID and owner.
     *
     * @param id     UUID of the category
     * @param userId the requesting user's UUID
     * @return the {@link CategoryResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found or owned by another user
     */
    public CategoryResponse findByIdForUser(UUID id, UUID userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}
