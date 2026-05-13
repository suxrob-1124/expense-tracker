package com.company.expensetracker.service.transaction;

import com.company.expensetracker.domain.Transaction;
import com.company.expensetracker.dto.transaction.TransactionPatchRequest;
import com.company.expensetracker.dto.transaction.TransactionRequest;
import com.company.expensetracker.dto.transaction.TransactionResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper between {@link Transaction} entity and its DTOs.
 *
 * <p>Null source properties are ignored during partial updates
 * ({@link NullValuePropertyMappingStrategy#IGNORE}) to support PATCH semantics.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    /** Maps a {@link Transaction} entity to its response DTO. */
    TransactionResponse toResponse(Transaction transaction);

    /**
     * Maps a {@link TransactionRequest} to a new {@link Transaction} entity.
     * The {@code userId} field is excluded and must be set by the caller.
     */
    @Mapping(target = "userId", ignore = true)
    Transaction toEntity(TransactionRequest request);

    /**
     * Applies non-null fields from {@code request} to the existing {@code transaction}.
     * Null fields are silently skipped ({@code NullValuePropertyMappingStrategy.IGNORE}).
     * The {@code userId} field is always excluded.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    void patchEntity(@MappingTarget Transaction transaction, TransactionPatchRequest request);
}
