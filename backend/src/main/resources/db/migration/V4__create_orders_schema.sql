CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    subtotal_currency CHAR(3) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    total_currency CHAR(3) NOT NULL,
    payment_id UUID,
    shipping_recipient_name VARCHAR(255) NOT NULL,
    shipping_address_line VARCHAR(255) NOT NULL,
    shipping_additional_info TEXT,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_department VARCHAR(100) NOT NULL,
    shipping_phone VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_orders_orders_order_number UNIQUE (order_number),
    CONSTRAINT ck_orders_orders_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED')
    ),
    CONSTRAINT ck_orders_orders_subtotal_amount CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_orders_orders_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_orders_subtotal_currency CHECK (subtotal_currency = 'COP'),
    CONSTRAINT ck_orders_orders_total_currency CHECK (total_currency = 'COP')
);

CREATE INDEX idx_orders_orders_customer_id ON orders.orders (customer_id);
CREATE INDEX idx_orders_orders_status ON orders.orders (status);
CREATE INDEX idx_orders_orders_created_at ON orders.orders (created_at);

CREATE TABLE orders.order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    item_index INTEGER NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price_amount NUMERIC(12, 2) NOT NULL,
    unit_price_currency CHAR(3) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    subtotal_currency CHAR(3) NOT NULL,
    CONSTRAINT fk_orders_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders.orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_orders_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_orders_order_items_unit_price CHECK (unit_price_amount >= 0),
    CONSTRAINT ck_orders_order_items_subtotal CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_orders_order_items_unit_price_currency CHECK (unit_price_currency = 'COP'),
    CONSTRAINT ck_orders_order_items_subtotal_currency CHECK (subtotal_currency = 'COP'),
    CONSTRAINT ck_orders_order_items_index CHECK (item_index >= 0)
);

CREATE INDEX idx_orders_order_items_order_id ON orders.order_items (order_id);
