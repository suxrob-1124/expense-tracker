package com.company.expensetracker.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    protected AuditEvent() {}

    public AuditEvent(String eventType, UUID userId, String payload, Instant occurredAt, String ipAddress) {
        this.eventType = eventType;
        this.userId = userId;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.ipAddress = ipAddress;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getEventType() { return eventType; }
    public UUID getUserId() { return userId; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getIpAddress() { return ipAddress; }
}
