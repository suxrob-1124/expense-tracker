package com.company.expensetracker.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String emailHash, Instant occurredAt) {}
