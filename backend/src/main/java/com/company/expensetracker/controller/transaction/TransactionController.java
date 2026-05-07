package com.company.expensetracker.controller.transaction;

import com.company.expensetracker.dto.transaction.TransactionPatchRequest;
import com.company.expensetracker.dto.transaction.TransactionRequest;
import com.company.expensetracker.dto.transaction.TransactionResponse;
import com.company.expensetracker.security.UserPrincipal;
import com.company.expensetracker.service.transaction.TransactionCommandService;
import com.company.expensetracker.service.transaction.TransactionQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

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

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody TransactionRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        TransactionResponse response = transactionCommandService.create(principal.userId(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/transactions/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal,
                                                            @RequestParam(required = false) Integer month,
                                                            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(transactionQueryService.findAllForUser(principal.userId(), month, year));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable UUID id) {
        return ResponseEntity.ok(transactionQueryService.findByIdForUser(id, principal.userId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody TransactionPatchRequest request) {
        return ResponseEntity.ok(transactionCommandService.update(id, principal.userId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable UUID id) {
        transactionCommandService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
