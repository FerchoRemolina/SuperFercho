CREATE TABLE shopping.favorites (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_shopping_favorites_customer_product UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_shopping_favorites_customer_created_at
    ON shopping.favorites (customer_id, created_at DESC);
