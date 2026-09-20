CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    refunded_at TIMESTAMPTZ,
    CONSTRAINT ck_payments_payments_amount CHECK (amount >= 0),
    CONSTRAINT ck_payments_payments_currency CHECK (currency = 'COP'),
    CONSTRAINT ck_payments_payments_method CHECK (
        payment_method IN ('SIMULATED_CARD', 'CASH_ON_DELIVERY')
    ),
    CONSTRAINT ck_payments_payments_status CHECK (
        status IN ('PENDING', 'APPROVED', 'DECLINED')
    )
);
