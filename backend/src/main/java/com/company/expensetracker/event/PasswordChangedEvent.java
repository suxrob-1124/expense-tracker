package com.company.expensetracker.event;

import java.time.Instant;
import java.util.UUID;

public record PasswordChangedEvent(UUID userId, Instant occurredAt) {}
