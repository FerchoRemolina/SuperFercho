-- delivered_at: momento de la transición real DELIVERY -> DELIVERED.
-- Backfill histórico: para los pedidos ya entregados el mejor dato disponible
-- es updated_at (DELIVERED es terminal; su última actualización fue la entrega).
-- Para pedidos nuevos el campo lo asigna el dominio en la transición.

ALTER TABLE orders.orders
    ADD COLUMN delivered_at timestamptz NULL;

UPDATE orders.orders
   SET delivered_at = updated_at
 WHERE status = 'DELIVERED'
   AND delivered_at IS NULL;
