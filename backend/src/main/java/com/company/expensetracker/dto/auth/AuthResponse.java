package com.company.expensetracker.dto.auth;

import com.company.expensetracker.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {}
