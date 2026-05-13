package com.company.expensetracker.service.user;

import com.company.expensetracker.domain.User;
import com.company.expensetracker.dto.user.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link com.company.expensetracker.domain.User} entities
 * and user-facing DTOs.
 *
 * <p>Uses the Spring component model ({@code componentModel = "spring"}).
 * Null source properties map to {@code null} in the target.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a {@link User} entity to a {@link UserResponse} DTO.
     * The {@code role} field is derived from {@link com.company.expensetracker.domain.Role#name()}.
     *
     * @param user the source entity
     * @return the mapped response DTO
     */
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);
}
