-- Contenido editable de la tienda (Fase 1: Hero principal, fila única).
-- updated_by es nullable: la fila semilla no referencia ningún admin
-- inexistente; el campo se llena en cada actualización del Admin.

CREATE SCHEMA IF NOT EXISTS storefront;

CREATE TABLE storefront.storefront_content (
    id UUID PRIMARY KEY,
    hero_title VARCHAR(120) NOT NULL,
    hero_description VARCHAR(400) NOT NULL,
    hero_cta_label VARCHAR(60) NOT NULL,
    hero_image_url VARCHAR(1024) NULL,
    hero_mode VARCHAR(20) NOT NULL,
    updated_by UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_storefront_content_mode CHECK (hero_mode IN ('STANDARD', 'IMMERSIVE'))
);

-- Fila singleton con el contenido actual del Hero (valores vigentes en la home).
INSERT INTO storefront.storefront_content (
    id, hero_title, hero_description, hero_cta_label,
    hero_image_url, hero_mode, updated_by, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Tu súper, más cerca de ti',
    'Compra tus productos, organiza el mercado y recibe ayuda cuando la necesites. Un supermercado digital, práctico y cercano.',
    'Ver productos',
    NULL,
    'STANDARD',
    NULL,
    now(),
    now()
);
