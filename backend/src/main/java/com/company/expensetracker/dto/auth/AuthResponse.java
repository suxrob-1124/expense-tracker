package com.company.expensetracker.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
