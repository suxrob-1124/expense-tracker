package com.company.expensetracker.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "date", nullable = false)
    private Instant date;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected Transaction() {}

    public Transaction(BigDecimal amount, TransactionType type, String description,
                       Instant date, UUID categoryId, UUID paymentMethodId, UUID userId) {
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.date = date;
        this.categoryId = categoryId;
        this.paymentMethodId = paymentMethodId;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public Instant getDate() { return date; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getPaymentMethodId() { return paymentMethodId; }
    public UUID getUserId() { return userId; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setType(TransactionType type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setDate(Instant date) { this.date = date; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public void setPaymentMethodId(UUID paymentMethodId) { this.paymentMethodId = paymentMethodId; }
}
