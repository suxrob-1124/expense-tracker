package com.company.expensetracker.repository;

import com.company.expensetracker.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for persisting {@link com.company.expensetracker.domain.AuditEvent} records.
 *
 * <p>Audit events are append-only write-through records created by application-event listeners
 * (e.g. on user registration, login, or failed login attempts). No custom query methods are
 * required because only inserts and bulk reads (for compliance export) are needed.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {}
