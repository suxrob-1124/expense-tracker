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

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "userId", ignore = true)
    Transaction toEntity(TransactionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    void patchEntity(@MappingTarget Transaction transaction, TransactionPatchRequest request);
}
