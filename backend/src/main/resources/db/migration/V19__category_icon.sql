ALTER TABLE catalog.categories
    ADD COLUMN icon VARCHAR(32) NOT NULL DEFAULT 'OTHER';

-- Backfill determinista por nombre para las categorías sembradas;
-- el resto conserva OTHER.
UPDATE catalog.categories SET icon = 'CLEANING' WHERE name ILIKE 'Aseo y cuidado del hogar%';
UPDATE catalog.categories SET icon = 'DRINKS' WHERE name ILIKE 'Bebidas%';
UPDATE catalog.categories SET icon = 'PERSONAL_CARE' WHERE name ILIKE 'Cuidado personal e higiene%';
UPDATE catalog.categories SET icon = 'GROCERY' WHERE name ILIKE 'Despensa, granos y básicos%';
UPDATE catalog.categories SET icon = 'FRUITS' WHERE name ILIKE 'Frutas y verduras%';
UPDATE catalog.categories SET icon = 'BAKERY' WHERE name ILIKE 'Panadería y pastelería%';
UPDATE catalog.categories SET icon = 'MEAT' WHERE name ILIKE 'Carnes, aves y pescados%';
UPDATE catalog.categories SET icon = 'DAIRY' WHERE name ILIKE 'Lácteos, huevos y refrigerados%';

ALTER TABLE catalog.categories
    ADD CONSTRAINT ck_catalog_categories_icon CHECK (icon IN (
        'CLEANING', 'DRINKS', 'PERSONAL_CARE', 'GROCERY', 'FRUITS', 'BAKERY',
        'MEAT', 'DAIRY', 'PETS', 'BABY', 'ELECTRONICS', 'HOME', 'OTHER'
    ));
