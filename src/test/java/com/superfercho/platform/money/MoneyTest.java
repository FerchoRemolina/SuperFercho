package com.superfercho.platform.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void shouldCreateCopAmount() {
        Money money = Money.cop(new BigDecimal("10.50"));

        assertThat(money.amount()).isEqualByComparingTo("10.50");
        assertThat(money.currency()).isEqualTo(Money.COP);
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.cop(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNonCopCurrency() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.00"), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Money.cop(new BigDecimal("10.123")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
