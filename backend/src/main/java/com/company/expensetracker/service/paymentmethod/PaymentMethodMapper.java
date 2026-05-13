package com.company.expensetracker.service.paymentmethod;

import com.company.expensetracker.domain.PaymentMethod;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodPatchRequest;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodRequest;
import com.company.expensetracker.dto.paymentmethod.PaymentMethodResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper between {@link PaymentMethod} entity and its DTOs.
 *
 * <p>For partial updates, null source properties are ignored
 * ({@link NullValuePropertyMappingStrategy#IGNORE}) so PATCH callers can
 * omit fields they don't want to change.
 */
@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    /** Maps a {@link PaymentMethod} entity to its response DTO. */
    PaymentMethodResponse toResponse(PaymentMethod paymentMethod);

    /**
     * Creates a new {@link PaymentMethod} from the request DTO.
     * The {@code userId} and {@code archived} fields are excluded —
     * {@code userId} is supplied by the service, {@code archived} defaults to {@code false}.
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "archived", ignore = true)
    PaymentMethod toEntity(PaymentMethodRequest request);

    /**
     * Applies non-null fields from {@code request} to the existing {@code paymentMethod}.
     * Null fields are skipped. The {@code userId} field is never updated.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    void patchEntity(@MappingTarget PaymentMethod paymentMethod, PaymentMethodPatchRequest request);
}
