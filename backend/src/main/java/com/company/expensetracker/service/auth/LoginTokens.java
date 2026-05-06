package com.company.expensetracker.service.auth;

import java.util.UUID;

public record LoginTokens(
        String accessToken,
        String refreshToken,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        UUID userId
) {}
