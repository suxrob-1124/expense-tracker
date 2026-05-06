package com.company.expensetracker.controller.category;

import com.company.expensetracker.dto.category.CategoryRequest;
import com.company.expensetracker.dto.category.CategoryResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.category.CategoryCommandService;
import com.company.expensetracker.service.category.CategoryQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

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

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CategoryRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        CategoryResponse response = categoryCommandService.create(principal.userId(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/categories/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryQueryService.findAllByUserId(principal.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(categoryQueryService.findByIdForUser(id, principal.userId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryCommandService.update(id, principal.userId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable UUID id) {
        categoryCommandService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
