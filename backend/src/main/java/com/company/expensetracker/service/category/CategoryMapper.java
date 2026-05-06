package com.company.expensetracker.service.category;

import com.company.expensetracker.domain.Category;
import com.company.expensetracker.dto.category.CategoryRequest;
import com.company.expensetracker.dto.category.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    @Mapping(target = "userId", ignore = true)
    Category toEntity(CategoryRequest request);

    void updateEntity(@MappingTarget Category category, CategoryRequest request);
}
