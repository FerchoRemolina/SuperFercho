-- Orders lifecycle: remove PENDING/READY; add DELIVERY.
-- Local inspection: no rows in PENDING/READY; CHECK-only migration.

ALTER TABLE orders.orders DROP CONSTRAINT ck_orders_orders_status;

ALTER TABLE orders.orders
    ADD CONSTRAINT ck_orders_orders_status CHECK (
        status IN ('CONFIRMED', 'PREPARING', 'DELIVERY', 'DELIVERED', 'CANCELLED')
    );

ALTER TABLE orders.checkout_idempotency DROP CONSTRAINT ck_orders_checkout_idempotency_order_status;

ALTER TABLE orders.checkout_idempotency
    ADD CONSTRAINT ck_orders_checkout_idempotency_order_status CHECK (
        result_order_status IN ('CONFIRMED', 'PREPARING', 'DELIVERY', 'DELIVERED', 'CANCELLED')
    );

CREATE INDEX IF NOT EXISTS idx_orders_orders_status_confirmed_at
    ON orders.orders (status, confirmed_at);
