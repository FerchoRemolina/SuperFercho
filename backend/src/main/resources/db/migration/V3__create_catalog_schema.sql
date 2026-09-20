CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_catalog_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_catalog_categories_status ON catalog.categories (status);

CREATE TABLE catalog.products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    barcode VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255),
    description TEXT,
    price_amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    stock INTEGER NOT NULL,
    image_url VARCHAR(1024),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_catalog_products_category
        FOREIGN KEY (category_id) REFERENCES catalog.categories (id),
    CONSTRAINT ck_catalog_products_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_catalog_products_price CHECK (price_amount >= 0),
    CONSTRAINT ck_catalog_products_stock CHECK (stock >= 0),
    CONSTRAINT ck_catalog_products_currency CHECK (currency = 'COP')
);

CREATE INDEX idx_catalog_products_category_id ON catalog.products (category_id);
CREATE INDEX idx_catalog_products_status ON catalog.products (status);
CREATE INDEX idx_catalog_products_category_status ON catalog.products (category_id, status);

CREATE UNIQUE INDEX uk_catalog_products_barcode
    ON catalog.products (barcode)
    WHERE barcode IS NOT NULL;
