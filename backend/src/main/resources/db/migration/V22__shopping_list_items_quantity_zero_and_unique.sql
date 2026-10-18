-- Las cantidades de productos en listas permiten 0: el producto permanece
-- guardado en la lista pero no se incluye al comprar la lista. El carrito
-- conserva su CHECK quantity > 0 sin cambios.
ALTER TABLE shopping.shopping_list_items DROP CONSTRAINT ck_shopping_shopping_list_items_quantity;

ALTER TABLE shopping.shopping_list_items
    ADD CONSTRAINT ck_shopping_shopping_list_items_quantity CHECK (quantity >= 0);

-- Un producto solo puede aparecer una vez por lista. La verificación previa
-- sobre la base local no encontró duplicados, así que no se requiere limpieza.
ALTER TABLE shopping.shopping_list_items
    ADD CONSTRAINT uk_shopping_shopping_list_items_list_product UNIQUE (shopping_list_id, product_id);
