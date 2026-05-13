package com.company.expensetracker.service.category;

import com.company.expensetracker.domain.Category;
import com.company.expensetracker.dto.category.CategoryRequest;
import com.company.expensetracker.dto.category.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between {@link com.company.expensetracker.domain.Category} entities
 * and category DTOs.
 *
 * <p>Uses the Spring component model ({@code componentModel = "spring"}).
 * Null source properties map to {@code null} in the target.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Maps a {@link Category} entity to a {@link CategoryResponse} DTO.
     *
     * @param category the source entity
     * @return the mapped response DTO
     */
    CategoryResponse toResponse(Category category);

    /**
     * Creates a new {@link Category} entity from the request DTO.
     * The {@code userId} field is excluded and must be set separately.
     *
     * @param request the category creation payload
     * @return a new entity without the {@code userId} field
     */
    @Mapping(target = "userId", ignore = true)
    Category toEntity(CategoryRequest request);

    /**
     * Applies the fields from the request DTO to an existing {@link Category} entity (full update).
     *
     * @param category the target entity to update
     * @param request  the new field values
     */
    void updateEntity(@MappingTarget Category category, CategoryRequest request);
}
