package com.company.expensetracker.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Persistent payment method owned by a single user (card, cash, or bank account).
 *
 * <p>Optimistic locking is enforced via {@link Version @Version} on {@link #version}.
 * Audit fields are inherited from {@link BaseEntity}.
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private PaymentMethodType type;

    @Column(name = "last4", length = 4)
    private String last4;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected PaymentMethod() {}

    public PaymentMethod(String name, PaymentMethodType type, String last4, BigDecimal balance, UUID userId) {
        this.name = name;
        this.type = type;
        this.last4 = last4;
        this.balance = balance;
        this.archived = false;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public PaymentMethodType getType() { return type; }
    public String getLast4() { return last4; }
    public BigDecimal getBalance() { return balance; }
    public boolean isArchived() { return archived; }
    public UUID getUserId() { return userId; }

    public void setName(String name) { this.name = name; }
    public void setType(PaymentMethodType type) { this.type = type; }
    public void setLast4(String last4) { this.last4 = last4; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
