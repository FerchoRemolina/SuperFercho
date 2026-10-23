-- Recibos durables de agregado idempotente al carrito (selección de paquetes 4E).
-- operation_id es el proposalId de la selección. Un UUID de propuesta no se reutiliza,
-- así que los recibos se conservan hasta la limpieza de preview o el cierre de cuenta.
-- No hay job de retención: no existe reuso de clave ni caducidad de idempotencia.
-- id es la PK de persistencia (UUID asignado al guardar) para que un INSERT concurrente
-- choque en uk_shopping_cart_add_operations_operation_id en lugar de fusionarse.
CREATE TABLE shopping.cart_add_operations (
    id UUID NOT NULL,
    operation_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_shopping_cart_add_operations PRIMARY KEY (id),
    CONSTRAINT uk_shopping_cart_add_operations_operation_id UNIQUE (operation_id),
    CONSTRAINT ck_shopping_cart_add_operations_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_shopping_cart_add_operations_customer_id
    ON shopping.cart_add_operations (customer_id);
