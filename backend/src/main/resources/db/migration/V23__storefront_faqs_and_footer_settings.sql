-- Etapa 1 configuración de Storefront: ajustes del footer en la fila
-- singleton existente y preguntas frecuentes persistidas.
-- Los valores iniciales del footer NO sobrescriben datos reales: la tabla es
-- nueva para FAQ y las columnas del footer usan DEFAULT con los valores
-- acordados (la fila singleton ya existente los adopta automáticamente en
-- PostgreSQL 11+ sin reescribir la fila).
ALTER TABLE storefront.storefront_content
    ADD COLUMN support_email VARCHAR(254) NOT NULL DEFAULT 'SoporteSuperFercho@gmail.com',
    ADD COLUMN linkedin_url VARCHAR(1024) NOT NULL DEFAULT 'https://www.linkedin.com/in/luis-remolina-0hh0/',
    ADD COLUMN github_url VARCHAR(1024) NOT NULL DEFAULT 'https://github.com/FerchoRemolina',
    ADD COLUMN qr_image_url VARCHAR(1024) NULL;

CREATE TABLE storefront.storefront_faqs (
    id UUID PRIMARY KEY,
    question VARCHAR(300) NOT NULL,
    answer VARCHAR(2000) NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_storefront_faqs_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_storefront_faqs_display_order
    ON storefront.storefront_faqs (display_order);

-- Seed de las 5 preguntas actuales de la tienda (órdenes espaciados para
-- permitir inserciones intermedias sin reordenar).
INSERT INTO storefront.storefront_faqs (
    id, question, answer, display_order, active, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-00000000fa01',
     '¿Cómo puedo realizar un pedido?',
     'Explora el catálogo o usa la búsqueda y agrega los productos que quieras a tu carrito. Desde el carrito revisa las cantidades y continúa al checkout: ahí seleccionas tu dirección de entrega y el método de pago, y confirmas tu pedido.',
     10, TRUE, now(), now()),
    ('00000000-0000-0000-0000-00000000fa02',
     '¿Qué métodos de pago están disponibles?',
     'Actualmente puedes pagar con tarjeta en modalidad simulada o en efectivo contra entrega. La modalidad de tarjeta es una simulación: no se procesan datos bancarios reales.',
     20, TRUE, now(), now()),
    ('00000000-0000-0000-0000-00000000fa03',
     '¿Cómo funciona el costo del domicilio?',
     'El costo del domicilio se acuerda directamente con el domiciliario y no está incluido en el total de los productos que muestra el checkout.',
     30, TRUE, now(), now()),
    ('00000000-0000-0000-0000-00000000fa04',
     '¿Cómo puedo cancelar un pedido?',
     'Desde el detalle del pedido puedes cancelarlo durante los primeros dos minutos posteriores a la confirmación, siempre que el pedido continúe en estado Confirmado. Vencido ese tiempo, ya no es posible cancelarlo desde la tienda.',
     40, TRUE, now(), now()),
    ('00000000-0000-0000-0000-00000000fa05',
     '¿Cómo puedo administrar mis direcciones?',
     'Entra a la sección Direcciones desde tu cuenta: ahí puedes agregar nuevas direcciones, editarlas, marcar una como predeterminada y desactivar las que ya no uses.',
     50, TRUE, now(), now());
