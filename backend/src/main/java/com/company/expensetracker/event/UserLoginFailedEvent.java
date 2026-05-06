package com.company.expensetracker.event;

import java.time.Instant;

public record UserLoginFailedEvent(String emailHash, Instant occurredAt, String reason) {}
