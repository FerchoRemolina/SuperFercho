ALTER TABLE catalog.products
    DROP CONSTRAINT ck_catalog_products_status;

ALTER TABLE catalog.products
    ADD CONSTRAINT ck_catalog_products_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'));
