# API de Analítica del Admin Hub

Contrato final de los endpoints backend que alimentan el módulo de Analítica.
Todos los períodos usan el contrato `[from, to)`: `from` inclusivo, `to`
exclusivo. El backend nunca requiere que el frontend calcule `23:59:59`.

## Zona horaria

Los períodos de negocio se definen en **America/Bogota**. Los buckets
temporales (día, mes, hora) se calculan con `date_trunc` sobre la hora local de
Bogota en base de datos. Una venta a las `2026-05-01T02:00:00Z` pertenece al
día `2026-04-30` (30 de abril, 21:00 en Bogota).

## Formato de fechas en los parámetros

`from` y `to` aceptan, en este orden:

1. ISO-8601 con offset/Z: `2026-05-01T05:00:00Z`, `2026-05-01T00:00:00-05:00`
2. Fecha-hora ingenua, interpretada en Bogota: `2026-05-01T00:00:00`
3. Fecha simple, interpretada como medianoche Bogota: `2026-05-01`

La respuesta serializa timestamps ISO-8601 UTC (formato estándar del proyecto).

## Validación y errores

Rango inválido (falta `from`/`to`, `from >= to`, granularidad desconocida,
rango excesivo) → `HTTP 400` ProblemDetail:

```json
{ "code": "INVALID_SALES_PERIOD", "detail": "from must be before to", ... }
```

En identity el código es `INVALID_ADMIN_CUSTOMER_QUERY` (estándar existente
del módulo).

Límites de rango documentados (evitar consultas gigantes):

| Granularidad | Rango máximo |
| ------------ | ------------ |
| `HOUR`       | 7 días       |
| `DAY`        | 400 días     |
| `MONTH`      | 5 años       |
| Sin buckets (summary, productos, clientes, clientes nuevos) | 5 años |

`limit` de rankings: default `5`, máximo `50` (se recorta, no se rechaza).

---

## 1. Ventas — `GET /api/v1/admin/orders/dashboard/sales`

### Modo legacy (rolling, sin `from`/`to`) — compatible, sin cambios

```
GET /api/v1/admin/orders/dashboard/sales?granularity=DAY|WEEK|MONTH|YEAR
```

Sin `granularity` se asume `WEEK`. Mantiene la respuesta y el comportamiento
históricos (últimas 24h / 7d / 30d / 12m).

### Modo período arbitrario (con `from` y `to`)

```
GET /api/v1/admin/orders/dashboard/sales
    ?from=2026-05-01T00:00:00
    &to=2026-06-01T00:00:00
    &granularity=HOUR|DAY|MONTH      (opcional, default DAY)
```

Respuesta (misma estructura que el modo legacy; `granularity` es el tamaño de
bucket, no la ventana rolling):

```json
{
  "granularity": "DAY",
  "buckets": [
    { "periodStart": "2026-05-01T05:00:00Z", "label": "01 may.", "total": { "amount": 125000.00, "currency": "COP" }, "orderCount": 8 }
  ]
}
```

Los buckets vacíos se rellenan con `total=0`, `orderCount=0` para continuidad
de gráfico. Regla de negocio: los pedidos `CANCELLED` no cuentan como ventas
(igual que el modo legacy).

## 2. Resumen de pedidos — `GET /api/v1/admin/orders/dashboard/summary`

```
GET /api/v1/admin/orders/dashboard/summary?from=2026-05-01&to=2026-06-01
```

```json
{
  "from": "2026-05-01T05:00:00Z",
  "to": "2026-06-01T05:00:00Z",
  "sales": { "amount": 1234500.00, "currency": "COP" },
  "totalOrders": 49,
  "inProcessOrders": 10,
  "deliveredOrders": 35,
  "cancelledOrders": 4
}
```

- `inProcessOrders` = `CONFIRMED + PREPARING + DELIVERY`
- `deliveredOrders` = `DELIVERED`
- `cancelledOrders` = `CANCELLED` (los cancelados sí aparecen aquí)
- `sales` = suma de `total` de pedidos **no cancelados** del período
- `totalOrders` = todos los estados

## 3. Productos más/menos vendidos — `GET /api/v1/admin/orders/dashboard/products`

```
GET /api/v1/admin/orders/dashboard/products
    ?from=2026-05-01&to=2026-06-01
    &limit=5          (opcional, default 5, max 50)
    &sort=DESC|ASC    (opcional, default DESC)
```

```json
{
  "items": [
    { "productId": "…", "productName": "Leche entera", "quantity": 25 }
  ]
}
```

- `quantity` = **unidades vendidas** (suma de `quantity` de los ítems), no
  cantidad de pedidos.
- `CANCELLED` no contribuye.
- `sort=DESC` → más vendidos; `sort=ASC` → menos vendidos.
- Limitación: solo aparecen productos con al menos una venta en el período
  (el catálogo vive en otro módulo; no se listan productos con cero ventas).

## 4. Clientes con mayor compra — `GET /api/v1/admin/orders/dashboard/customers/top`

```
GET /api/v1/admin/orders/dashboard/customers/top
    ?from=2026-05-01&to=2026-06-01
    &limit=5          (opcional, default 5, max 50)
```

```json
{
  "items": [
    {
      "customerId": "…",
      "customerName": "Ada Lovelace",
      "total": { "amount": 450000.00, "currency": "COP" },
      "orderCount": 6
    }
  ]
}
```

- Métrica principal: `total` comprado en el período, ordenado DESC.
- `CANCELLED` excluido de la suma y del conteo.
- `customerName` es el nombre del destinatario de envío (snapshot de la
  orden), misma semántica que `recent-buyers`.

## 5. Clientes nuevos — `GET /api/v1/admin/customers/dashboard/new`

```
GET /api/v1/admin/customers/dashboard/new
    ?from=2026-05-01T00:00:00
    &to=2026-06-01T00:00:00
    &granularity=HOUR|DAY|MONTH      (opcional, default DAY)
```

```json
{
  "total": 12,
  "buckets": [
    { "periodStart": "2026-05-01T05:00:00Z", "label": "01 may.", "count": 4 }
  ]
}
```

- Basado en la **creación real del `CustomerRecord`** (registro comercial), no
  en la primera compra.
- Las cuentas de storefront-preview y los re-registros sobre un registro
  comercial existente no cuentan como clientes nuevos.
- Buckets vacíos rellenados con `count=0`.

---

## Mapeo sugerido del selector del frontend (responsabilidad del frontend)

| Selector     | from / to (America/Bogota)              | granularity |
| ------------ | --------------------------------------- | ----------- |
| Hoy          | inicio/fin del día actual               | HOUR        |
| Esta semana  | lunes 00:00 / lunes siguiente 00:00     | DAY         |
| Este mes     | 1º del mes / 1º del mes siguiente       | DAY         |
| Este año     | 1 ene / 1 ene siguiente                 | MONTH       |
| Personalizado| fechas del usuario                      | a criterio  |

## Notas de rendimiento

- Todas las agregaciones (GROUP BY / SUM / COUNT / ORDER BY / LIMIT) se
  ejecutan en PostgreSQL; el backend no descarga órdenes para agregar en
  memoria.
- Queries nativas sobre `orders.orders` / `orders.order_items` /
  `identity.customer_records`; índices usados: `idx_orders_orders_created_at`,
  `idx_orders_order_items_order_id`,
  `idx_identity_customer_records_created_at` (nuevo, migración V18).
- El relleno de buckets vacíos es acotado por los límites de rango (máx ~500
  buckets).
