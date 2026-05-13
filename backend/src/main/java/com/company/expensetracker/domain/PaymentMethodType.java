package com.company.expensetracker.domain;

/**
 * Enumerates the kinds of user payment methods.
 *
 * <ul>
 *   <li>{@link #CARD} — credit or debit card; usually carries a {@code last4} suffix.</li>
 *   <li>{@link #CASH} — physical cash; never has {@code last4}.</li>
 *   <li>{@link #BANK} — bank account; may carry a {@code last4} account suffix.</li>
 * </ul>
 */
public enum PaymentMethodType {
    CARD,
    CASH,
    BANK
}
