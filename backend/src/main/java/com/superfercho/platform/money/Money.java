package com.superfercho.platform.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable monetary value for the MVP. Currency is COP only.
 */
public record Money(BigDecimal amount, String currency) {

    public static final String COP = "COP";

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (!COP.equals(currency)) {
            throw new IllegalArgumentException("MVP supports COP only");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("amount must have at most two decimal places", ex);
        }
    }

    public static Money cop(BigDecimal amount) {
        return new Money(amount, COP);
    }
}
