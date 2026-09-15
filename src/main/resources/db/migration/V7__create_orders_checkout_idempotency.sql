CREATE TABLE orders.checkout_idempotency (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    idempotency_key TEXT NOT NULL,
    fingerprint TEXT NOT NULL,
    result_order_id UUID NOT NULL,
    result_order_number VARCHAR(64) NOT NULL,
    result_order_status VARCHAR(20) NOT NULL,
    result_payment_status VARCHAR(20) NOT NULL,
    result_total_amount NUMERIC(12, 2) NOT NULL,
    result_total_currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_orders_checkout_idempotency_customer_key UNIQUE (customer_id, idempotency_key),
    CONSTRAINT ck_orders_checkout_idempotency_order_status CHECK (
        result_order_status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED')
    ),
    CONSTRAINT ck_orders_checkout_idempotency_payment_status CHECK (
        result_payment_status IN ('PENDING', 'APPROVED', 'DECLINED')
    ),
    CONSTRAINT ck_orders_checkout_idempotency_total_amount CHECK (result_total_amount >= 0),
    CONSTRAINT ck_orders_checkout_idempotency_total_currency CHECK (result_total_currency = 'COP')
);

CREATE INDEX idx_orders_checkout_idempotency_expires_at
    ON orders.checkout_idempotency (expires_at);
