package com.company.expensetracker.controller.paymentmethod;

import com.company.expensetracker.dto.paymentmethod.PaymentMethodPatchRequest;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodRequest;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.paymentmethod.PaymentMethodCommandService;
import com.company.expensetracker.service.paymentmethod.PaymentMethodQueryService;
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
 * REST controller for managing user-owned payment methods.
 *
 * <p>Base path: {@code /api/v1/payment-methods}. All endpoints require authentication.
 * Payment methods are user-scoped — each user can only access their own.
 *
 * <p>Write operations are delegated to {@link PaymentMethodCommandService};
 * read operations to {@link PaymentMethodQueryService}.
 */
@Tag(name = "Payment Methods", description = "CRUD operations for user-owned payment methods (cards, cash, bank accounts).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodCommandService paymentMethodCommandService;
    private final PaymentMethodQueryService paymentMethodQueryService;

    public PaymentMethodController(PaymentMethodCommandService paymentMethodCommandService,
                                   PaymentMethodQueryService paymentMethodQueryService) {
        this.paymentMethodCommandService = paymentMethodCommandService;
        this.paymentMethodQueryService = paymentMethodQueryService;
    }

    /**
     * Creates a new payment method for the authenticated user.
     *
     * @param principal  the authenticated user
     * @param request    payment method payload
     * @param uriBuilder URI builder for the {@code Location} header
     * @return {@code 201 Created} with the created {@link PaymentMethodResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 409} if a payment method with the same name already exists
     */
    @Operation(summary = "Create a payment method",
            description = "Creates a new user-owned payment method (CARD, CASH, or BANK).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment method created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "409", description = "Payment method name already exists for this user"),
    })
    @PostMapping
    public ResponseEntity<PaymentMethodResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody PaymentMethodRequest request,
                                                        UriComponentsBuilder uriBuilder) {
        PaymentMethodResponse response = paymentMethodCommandService.create(principal.userId(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/payment-methods/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Returns all payment methods belonging to the authenticated user, sorted by name.
     *
     * @param principal the authenticated user
     * @return list of {@link PaymentMethodResponse}; may be empty
     */
    @Operation(summary = "List payment methods",
            description = "Returns all payment methods owned by the authenticated user, sorted by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of payment methods"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
    })
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentMethodQueryService.findAllByUserId(principal.userId()));
    }

    /**
     * Returns a single payment method by UUID.
     *
     * @param principal the authenticated user
     * @param id        UUID of the payment method
     * @return the {@link PaymentMethodResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found or belongs to another user
     */
    @Operation(summary = "Get a payment method",
            description = "Returns the payment method with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment method found"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Payment method not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Payment method UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(paymentMethodQueryService.findByIdForUser(id, principal.userId()));
    }

    /**
     * Partially updates a payment method (PATCH semantics).
     *
     * <p>Only non-null fields in the body are applied. Use {@code archived: true|false}
     * to toggle archive state — there is no dedicated archive endpoint.
     *
     * @param principal the authenticated user
     * @param id        UUID of the payment method to update
     * @param request   fields to update; null fields are ignored
     * @return the updated {@link PaymentMethodResponse}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found,
     *         {@code 409} if the new name conflicts with an existing payment method
     */
    @Operation(summary = "Partially update a payment method",
            description = "Applies only non-null fields from the request. Use 'archived' to toggle archive state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment method updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Payment method not found"),
            @ApiResponse(responseCode = "409", description = "New name conflicts with an existing payment method"),
    })
    @PatchMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> update(@AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Payment method UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
                                                        @Valid @RequestBody PaymentMethodPatchRequest request) {
        return ResponseEntity.ok(paymentMethodCommandService.update(id, principal.userId(), request));
    }

    /**
     * Deletes a payment method by UUID.
     *
     * <p>Transactions linked to this payment method are preserved — their
     * {@code paymentMethodId} field becomes {@code null} via {@code ON DELETE SET NULL}.
     *
     * @param principal the authenticated user
     * @param id        UUID of the payment method
     * @return {@code 204 No Content}
     * @throws org.springframework.web.server.ResponseStatusException {@code 404} if not found or belongs to another user
     */
    @Operation(summary = "Delete a payment method",
            description = "Deletes the payment method. Linked transactions remain but lose the reference (SET NULL).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payment method deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Payment method not found"),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Payment method UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        paymentMethodCommandService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
