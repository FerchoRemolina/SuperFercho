CREATE SCHEMA IF NOT EXISTS shopping;

CREATE TABLE shopping.carts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_shopping_carts_customer_id UNIQUE (customer_id),
    CONSTRAINT ck_shopping_carts_status CHECK (status IN ('ACTIVE'))
);

CREATE TABLE shopping.cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    item_index INTEGER NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_addition_amount NUMERIC(12, 2) NOT NULL,
    price_at_addition_currency CHAR(3) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_shopping_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES shopping.carts (id) ON DELETE CASCADE,
    CONSTRAINT ck_shopping_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_shopping_cart_items_price CHECK (price_at_addition_amount >= 0),
    CONSTRAINT ck_shopping_cart_items_currency CHECK (price_at_addition_currency = 'COP'),
    CONSTRAINT ck_shopping_cart_items_index CHECK (item_index >= 0)
);

CREATE INDEX idx_shopping_cart_items_cart_id ON shopping.cart_items (cart_id);

CREATE TABLE shopping.shopping_lists (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shopping_shopping_lists_customer_id ON shopping.shopping_lists (customer_id);

CREATE TABLE shopping.shopping_list_items (
    id UUID PRIMARY KEY,
    shopping_list_id UUID NOT NULL,
    item_index INTEGER NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_shopping_shopping_list_items_list
        FOREIGN KEY (shopping_list_id) REFERENCES shopping.shopping_lists (id) ON DELETE CASCADE,
    CONSTRAINT ck_shopping_shopping_list_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_shopping_shopping_list_items_index CHECK (item_index >= 0)
);

CREATE INDEX idx_shopping_shopping_list_items_list_id ON shopping.shopping_list_items (shopping_list_id);
