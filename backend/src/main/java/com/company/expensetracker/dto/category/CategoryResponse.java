package com.company.expensetracker.dto.category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String color,
        String icon,
        Instant createdAt,
        Instant updatedAt
) {}
