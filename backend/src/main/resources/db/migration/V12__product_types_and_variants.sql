CREATE TABLE catalog.product_types (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_catalog_product_types_category
        FOREIGN KEY (category_id) REFERENCES catalog.categories (id),
    CONSTRAINT ck_catalog_product_types_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uk_catalog_product_types_category_name UNIQUE (category_id, name)
);

CREATE INDEX idx_catalog_product_types_category_id ON catalog.product_types (category_id);
CREATE INDEX idx_catalog_product_types_status ON catalog.product_types (status);
CREATE INDEX idx_catalog_product_types_category_status ON catalog.product_types (category_id, status);

CREATE TABLE catalog.product_variants (
    id UUID PRIMARY KEY,
    product_type_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_catalog_product_variants_type
        FOREIGN KEY (product_type_id) REFERENCES catalog.product_types (id),
    CONSTRAINT ck_catalog_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uk_catalog_product_variants_type_name UNIQUE (product_type_id, name),
    CONSTRAINT uk_catalog_product_variants_id_type UNIQUE (id, product_type_id)
);

CREATE INDEX idx_catalog_product_variants_product_type_id ON catalog.product_variants (product_type_id);
CREATE INDEX idx_catalog_product_variants_status ON catalog.product_variants (status);
CREATE INDEX idx_catalog_product_variants_type_status ON catalog.product_variants (product_type_id, status);

ALTER TABLE catalog.products
    ADD COLUMN product_type_id UUID,
    ADD COLUMN product_variant_id UUID,
    ADD COLUMN presentation_quantity NUMERIC(12, 3),
    ADD COLUMN presentation_unit VARCHAR(20);

INSERT INTO catalog.product_types (id, category_id, name, description, status, created_at, updated_at)
SELECT gen_random_uuid(),
       c.id,
       c.name || ' — general',
       'Legacy type created during catalog taxonomy migration.',
       'ACTIVE',
       NOW(),
       NOW()
FROM catalog.categories c
WHERE EXISTS (SELECT 1 FROM catalog.products p WHERE p.category_id = c.id)
  AND NOT EXISTS (
      SELECT 1
      FROM catalog.product_types t
      WHERE t.category_id = c.id
        AND t.name = c.name || ' — general'
  );

UPDATE catalog.products p
SET product_type_id = t.id,
    presentation_quantity = 1,
    presentation_unit = 'UNIT'
FROM catalog.product_types t
WHERE t.category_id = p.category_id
  AND t.name = (SELECT c.name || ' — general' FROM catalog.categories c WHERE c.id = p.category_id)
  AND p.product_type_id IS NULL;

ALTER TABLE catalog.products
    ALTER COLUMN product_type_id SET NOT NULL,
    ALTER COLUMN presentation_quantity SET NOT NULL,
    ALTER COLUMN presentation_unit SET NOT NULL;

ALTER TABLE catalog.products
    ADD CONSTRAINT fk_catalog_products_product_type
        FOREIGN KEY (product_type_id) REFERENCES catalog.product_types (id),
    ADD CONSTRAINT fk_catalog_products_variant_type
        FOREIGN KEY (product_variant_id, product_type_id)
        REFERENCES catalog.product_variants (id, product_type_id),
    ADD CONSTRAINT ck_catalog_products_presentation_quantity CHECK (presentation_quantity > 0),
    ADD CONSTRAINT ck_catalog_products_presentation_unit
        CHECK (presentation_unit IN ('G', 'KG', 'ML', 'L', 'UNIT', 'PACK', 'BOX', 'ROLL'));

CREATE INDEX idx_catalog_products_product_type_id ON catalog.products (product_type_id);
CREATE INDEX idx_catalog_products_product_variant_id ON catalog.products (product_variant_id);
