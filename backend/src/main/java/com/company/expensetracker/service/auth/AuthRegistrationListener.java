package com.company.expensetracker.service.auth;

import com.company.expensetracker.domain.AuditEvent;
import com.company.expensetracker.event.UserRegisteredEvent;
import com.company.expensetracker.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Spring event listener that persists a {@code USER_REGISTERED} audit record
 * after the user registration transaction commits.
 *
 * <p>Uses {@code @TransactionalEventListener(AFTER_COMMIT)} so the audit entry
 * is only written after the outer transaction succeeds. Runs in a new
 * {@code REQUIRES_NEW} transaction to avoid contaminating the caller.
 */
@Component
public class AuthRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(AuthRegistrationListener.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuthRegistrationListener(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles a {@link com.company.expensetracker.event.UserRegisteredEvent} by saving
     * a {@code USER_REGISTERED} {@link com.company.expensetracker.domain.AuditEvent}.
     *
     * @param event the registration event containing userId, emailHash and timestamp
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserRegistered(UserRegisteredEvent event) {
        String payload = serializePayload(Map.of(
                "userId", event.userId().toString(),
                "emailHash", event.emailHash()
        ));
        auditEventRepository.save(new AuditEvent(
                "USER_REGISTERED",
                event.userId(),
                payload,
                event.occurredAt(),
                null
        ));
    }

    private String serializePayload(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit payload", e);
            return "{}";
        }
    }
}
