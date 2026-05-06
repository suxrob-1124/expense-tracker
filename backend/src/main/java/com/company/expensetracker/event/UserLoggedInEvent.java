package com.company.expensetracker.event;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedInEvent(UUID userId, Instant occurredAt, String ipAddress) {}
