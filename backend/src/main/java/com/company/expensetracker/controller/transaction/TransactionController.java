package com.company.expensetracker.controller.transaction;

import com.company.expensetracker.dto.common.PagedResponse;
import com.company.expensetracker.dto.transaction.TransactionPatchRequest;
import com.company.expensetracker.dto.transaction.TransactionRequest;
import com.company.expensetracker.dto.transaction.TransactionResponse;
import com.company.expensetracker.dto.transaction.TransactionSummaryResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.transaction.TransactionCommandService;
import com.company.expensetracker.service.transaction.TransactionQueryService;
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
 * REST controller for managing user financial transactions.
 *
 * <p>Base path: {@code /api/v1/transactions}. All endpoints require
 * JWT Bearer authentication with role {@code USER}.
 *
 * <p>Write operations are delegated to {@link TransactionCommandService};
 * read operations are delegated to {@link TransactionQueryService}.
 */
@Tag(name = "Transactions", description = "Manage personal financial transactions")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionCommandService transactionCommandService;
    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransactionCommandService transactionCommandService,
                                 TransactionQueryService transactionQueryService) {
        this.transactionCommandService = transactionCommandService;
        this.transactionQueryService = transactionQueryService;
    }

    /**
     * Creates a new transaction.
     *
     * <p>Verifies that the specified category belongs to the authenticated user
     * before persisting. Returns a {@code Location} header pointing to the created resource.
     *
     * @param principal  the authenticated user
     * @param request    transaction payload
     * @param uriBuilder URI builder for the {@code Location} response header
     * @return 201 Created with the created {@link TransactionResponse} body
     * @throws org.springframework.web.server.ResponseStatusException 404 if the category is not found or belongs to another user
     */
    @Operation(summary = "Create a transaction", description = "Creates a new transaction and returns it with a generated UUID and timestamps.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Category not found or belongs to another user")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody TransactionRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        TransactionResponse response = transactionCommandService.create(principal.userId(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/transactions/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Returns a paginated list of the user's most recent transactions.
     *
     * @param principal the authenticated user
     * @param page      zero-based page number (default 0)
     * @param size      page size between 1 and 50 (default 10)
     * @return page of transactions sorted by date descending
     * @throws IllegalArgumentException if {@code page < 0} or {@code size} is outside 1–50
     */
    @Operation(summary = "List latest transactions", description = "Returns transactions with pagination, sorted by date (newest first).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of transactions"),
            @ApiResponse(responseCode = "400", description = "Invalid page or size parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @GetMapping("/latest")
    public ResponseEntity<PagedResponse<TransactionResponse>> getLatest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Zero-based page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1–50)", example = "10") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionQueryService.findLatest(principal.userId(), page, size));
    }

    /**
     * Returns all transactions for the given month.
     *
     * <p>When {@code month} and {@code year} are omitted, defaults to the current UTC month.
     *
     * @param principal the authenticated user
     * @param month     month number 1–12 (optional)
     * @param year      calendar year (optional, used together with {@code month})
     * @return list of transactions sorted by date descending
     */
    @Operation(summary = "List transactions by month", description = "Returns all transactions for the specified month. Defaults to the current UTC month when parameters are omitted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of transactions"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Month number (1–12)", example = "5") @RequestParam(required = false) Integer month,
            @Parameter(description = "Calendar year", example = "2026") @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(transactionQueryService.findAllForUser(principal.userId(), month, year));
    }

    /**
     * Returns an income/expense/balance summary for the given month.
     *
     * <p>Defaults to the current UTC month when parameters are omitted.
     *
     * @param principal the authenticated user
     * @param month     month number 1–12 (optional)
     * @param year      calendar year (optional)
     * @return {@link TransactionSummaryResponse} with {@code income}, {@code expense}, and {@code balance}
     */
    @Operation(summary = "Monthly financial summary", description = "Aggregates income and expenses for the month. Defaults to the current UTC month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Financial summary"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryResponse> summary(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Month number (1–12)", example = "5") @RequestParam(required = false) Integer month,
            @Parameter(description = "Calendar year", example = "2026") @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(transactionQueryService.summarize(principal.userId(), month, year));
    }

    /**
     * Returns a single transaction by ID.
     *
     * @param principal the authenticated user
     * @param id        UUID of the transaction
     * @return the transaction
     * @throws org.springframework.web.server.ResponseStatusException 404 if the transaction does not exist or belongs to another user
     */
    @Operation(summary = "Get a transaction", description = "Returns the transaction with the given UUID. Returns 404 if not found or owned by another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Transaction UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        return ResponseEntity.ok(transactionQueryService.findByIdForUser(id, principal.userId()));
    }

    /**
     * Partially updates a transaction (PATCH semantics).
     *
     * <p>Only non-null fields in the request body are applied via MapStruct
     * ({@code NullValuePropertyMappingStrategy.IGNORE}). If {@code categoryId} changes,
     * ownership of the new category is verified. Uses optimistic locking via {@code @Version}.
     *
     * @param principal the authenticated user
     * @param id        UUID of the transaction to update
     * @param request   fields to update; null fields are ignored
     * @return the updated transaction
     * @throws org.springframework.web.server.ResponseStatusException 404 if the transaction or the new category is not found/owned
     */
    @Operation(summary = "Partially update a transaction", description = "Applies only non-null fields from the request. Null fields are ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Transaction or category not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Transaction UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id,
            @Valid @RequestBody TransactionPatchRequest request) {
        return ResponseEntity.ok(transactionCommandService.update(id, principal.userId(), request));
    }

    /**
     * Deletes a transaction by ID.
     *
     * <p>Verifies ownership before deletion.
     *
     * @param principal the authenticated user
     * @param id        UUID of the transaction
     * @return 204 No Content
     * @throws org.springframework.web.server.ResponseStatusException 404 if the transaction does not exist or belongs to another user
     */
    @Operation(summary = "Delete a transaction", description = "Deletes the transaction. Returns 204 on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Transaction UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        transactionCommandService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
