package com.company.expensetracker.service.category;

import com.company.expensetracker.domain.Category;
import com.company.expensetracker.dto.category.CategoryRequest;
import com.company.expensetracker.dto.category.CategoryResponse;
import com.company.expensetracker.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Write-side CQRS service for category mutations.
 *
 * <p>All operations are {@code @Transactional} and require {@code ROLE_USER}.
 * Every operation verifies that the target category belongs to the requesting user
 * via {@link CategoryRepository#findByIdAndUserId}.
 */
@Service
@Transactional
@PreAuthorize("hasRole('USER')")
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryCommandService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /**
     * Creates a new category owned by the given user.
     *
     * @param userId  the owner's UUID
     * @param request category payload (name, color, icon)
     * @return the persisted category as a {@link CategoryResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 409} if a category with the same name already exists for this user
     */
    public CategoryResponse create(UUID userId, CategoryRequest request) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        Category category = new Category(request.name(), request.color(), request.icon(), userId);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Fully updates a category after verifying ownership.
     *
     * @param id      UUID of the category to update
     * @param userId  the requesting user's UUID
     * @param request new category data
     * @return the updated {@link CategoryResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the category is not found or owned by another user,
     *         {@code 409} if the new name conflicts with an existing category for this user
     */
    public CategoryResponse update(UUID id, UUID userId, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        categoryMapper.updateEntity(category, request);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Deletes a category after verifying ownership.
     *
     * @param id     UUID of the category to delete
     * @param userId the requesting user's UUID
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the category is not found or owned by another user
     */
    public void delete(UUID id, UUID userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        categoryRepository.delete(category);
    }
}
