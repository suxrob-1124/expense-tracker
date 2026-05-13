package com.company.expensetracker.controller.category;

import com.company.expensetracker.dto.category.CategoryRequest;
import com.company.expensetracker.dto.category.CategoryResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.category.CategoryCommandService;
import com.company.expensetracker.service.category.CategoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing user-defined categories.
 *
 * <p>Base path: {@code /api/v1/categories}. All endpoints require authentication.
 * Categories are user-scoped — each user can only access their own categories.
 *
 * <p>Write operations are delegated to {@link CategoryCommandService};
 * read operations to {@link CategoryQueryService}.
 */
@Tag(name = "Categories", description = "CRUD operations for user-defined expense/income categories.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;

    public CategoryController(CategoryCommandService categoryCommandService,
                              CategoryQueryService categoryQueryService) {
        this.categoryCommandService = categoryCommandService;
        this.categoryQueryService = categoryQueryService;
    }

    /**
     * Creates a new category for the authenticated user.
     *
     * <p>Returns a {@code Location} header pointing to the created resource.
     *
     * @param principal  the authenticated user
     * @param request    category payload
     * @param uriBuilder URI builder for the {@code Location} header
     * @return {@code 201 Created} with the created {@link CategoryResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 409} if a category with the same name already exists
     */
    @Operation(summary = "Create a category", description = "Creates a new user-owned category.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "409", description = "Category name already exists for this user"),
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CategoryRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        CategoryResponse response = categoryCommandService.create(principal.userId(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/categories/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Returns all categories belonging to the authenticated user.
     *
     * @param principal the authenticated user
     * @return list of {@link CategoryResponse} sorted by name ascending; may be empty
     */
    @Operation(summary = "List categories", description = "Returns all categories owned by the authenticated user, sorted by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of categories"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryQueryService.findAllByUserId(principal.userId()));
    }

    /**
     * Returns a single category by UUID.
     *
     * @param principal the authenticated user
     * @param id        UUID of the category
     * @return the {@link CategoryResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found or belongs to another user
     */
    @Operation(summary = "Get a category", description = "Returns the category with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(categoryQueryService.findByIdForUser(id, principal.userId()));
    }

    /**
     * Replaces a category's fields (full update).
     *
     * @param principal the authenticated user
     * @param id        UUID of the category to update
     * @param request   new category data
     * @return the updated {@link CategoryResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found,
     *         {@code 409} if the new name conflicts with an existing category
     */
    @Operation(summary = "Update a category", description = "Replaces the category data. Name must remain unique for the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "New name conflicts with an existing category"),
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
                                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryCommandService.update(id, principal.userId(), request));
    }

    /**
     * Deletes a category by UUID.
     *
     * @param principal the authenticated user
     * @param id        UUID of the category
     * @return {@code 204 No Content}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found or belongs to another user
     */
    @Operation(summary = "Delete a category", description = "Deletes the category. Returns 204 on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        categoryCommandService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
