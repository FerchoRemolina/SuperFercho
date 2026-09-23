# SuperFercho Frontend Technical Architecture Blueprint

**Estado:** Arquitectura de referencia del frontend MVP implementado en `frontend/`

**Tipo:** Documentación técnica (alineada al código y al backend blueprint)

**Fuente de verdad del backend:** código REST, seguridad y `docs/architecture/backend-technical-blueprint.md`

**Fuente de verdad del frontend:** código bajo `frontend/` (App Router, features, shared)

**Inspección / alineación documental:** 2026-09-22

Leyenda de decisiones:

| Etiqueta | Significado |
|---|---|
| **DECIDIDO** | Encaja con el backend actual y está implementado (o sigue siendo la base vigente) |
| **PROPUESTO** | Recomendación técnica; se puede sustituir sin cambiar contratos REST |
| **PENDIENTE** | Depende de infraestructura externa o de un cambio de backend aún no existente |
| **FUTURO / fuera del MVP** | Decidido no implementar en el MVP REST o UI actuales |

Este documento describe el frontend existente y los contratos REST reales. No inventa endpoints, DTOs ni reglas de negocio.

---

## 0. Hallazgo de inspección

**DECIDIDO (hecho observado):** existe `frontend/` como aplicación Next.js (App Router) en el mismo repositorio, junto a `backend/` y `docs/`.

Layout vigente:

```text
SuperFercho/
├── backend/      # módulo Maven
├── frontend/     # Next.js App Router + features + shared
├── docs/
└── README.md
```

No monorepo tooling (sin Turborepo, Nx ni workspaces npm). Alias `@/*` → `frontend/*`.

El frontend es un cliente HTTP del API `/api/v1`. No duplica Domain, no habla con PostgreSQL y no reimplementa vendibilidad, stock atómico de checkout ni Assistant.
---

## 1. Stack

### Evaluación

El backend es REST + JWT Bearer + `application/problem+json`. Cualquier SPA o SSR que hable HTTP sirve. Criterios: routing (público / CUSTOMER / ADMIN), catálogo indexable, TypeScript alineado a DTOs Jackson, testing, despliegue junto a un API Spring, y portafolio profesional.

| Opción | Encaje | Motivo de rechazo o adopción |
|---|---|---|
| React + Next.js (App Router) + TypeScript | Alto | Rutas por convención, layouts por rol, proxy same-origin, SSR/SSG del catálogo público, ecosistema de tests |
| React SPA (Vite) + TypeScript | Alto | Más simple, pero SEO del catálogo y proxy/CORS quedan más a mano |
| Angular | Medio | Más ceremonia para un MVP de portafolio |
| Vue / Nuxt | Medio | Igual de válido; menos alineado al stack ya citado en el briefing |

**DECIDIDO:** React + Next.js App Router + TypeScript + Tailwind CSS 4 + TanStack Query + pnpm.

Por qué Next.js:

- El catálogo público (`GET /api/v1/products`, `GET /api/v1/categories`) no exige JWT y se beneficia de HTML inicial.
- App Router cubre layouts público / cuenta / admin sin un router ad hoc.
- `fetch` nativo basta para REST; no hace falta un segundo framework HTTP.
- TypeScript modela los records REST reales (`Money`, enums Jackson `STRING`).
- Un rewrite de Next.js evita CORS en desarrollo (el backend **no** configura CORS hoy).

### 1.1 Versiones (alineadas a `frontend/package.json`)

| Paquete | Versión | Motivo |
|---|---|---|
| Next.js | **16.3.5** | App Router, `engines.node >= 20.9.0`, peer `react@^19` |
| React | **19.3.0** | peer de Next 16 |
| React DOM | **19.3.0** | misma línea que React |
| TypeScript | **5.9.3** | Next 16 exige ≥ 5.1.0 |
| Tailwind CSS | **4.3.3** | línea 4 aprobada |
| `@tailwindcss/postcss` | **4.3.3** | plugin oficial v4 |
| `@tanstack/react-query` | **5.103.1** | línea 5 |
| `@types/react` / `@types/react-dom` | **19.3.x** | alineados a React 19.3 |
| Node.js | **>= 20.9.0** (LTS en desarrollo) | mínimo Next |
| pnpm | **12.4.2** | `packageManager` del proyecto |

**DECIDIDO (gestor):** pnpm. Sin npm/yarn/bun como herramienta principal.

**Rechazado en el MVP frontend:** Redux, Axios, tRPC, GraphQL, Prisma, ORM contra PostgreSQL, Spring AI en el cliente, WebSockets (el Assistant es `POST` síncrono), microfrontends, Native mobile, Material UI, Chakra, Ant Design, Bootstrap, shadcn/ui, Turborepo, Nx.

---

## 2. Arquitectura

**DECIDIDO:** combinación **App Router (rutas/layouts) + features + capa shared**.

```text
app/            → rutas, layouts, composición de UI
features/       → un bounded context de UI por módulo de negocio
shared/         → API client, auth session, UI kit, errores, money
```

Las features siguen los módulos del backend, no los duplican:

| Feature UI | Backend dueño | Habla con |
|---|---|---|
| `catalog` | Catalog | `/api/v1/products`, `/api/v1/categories` |
| `auth` | Identity | `/api/v1/auth/login`, `/api/v1/customers` |
| `account` | Identity | `/api/v1/addresses` |
| `cart` | Shopping | `/api/v1/cart` |
| `favorites` | Shopping | `/api/v1/favorites` |
| `lists` | Shopping | `/api/v1/shopping-lists` |
| `checkout` / `orders` | Orders | `/api/v1/orders` |
| `admin-orders` | Orders | `GET /api/v1/admin/orders`, `GET /api/v1/admin/orders/{orderId}` |
| `admin-catalog` | Catalog | mutaciones + `view=ADMIN` |
| `admin-knowledge` | Knowledge | `/api/v1/knowledge/**` |
| `admin-payments` | Payments | `GET /api/v1/payments/{paymentId}` |
| `assistant` | Assistant | `POST /api/v1/assistant/chat` |

Reglas:

- Las páginas no llaman `fetch` directo.
- Un único API client parsea `application/problem+json`.
- Los tipos HTTP viven junto al client, copiados de los records REST (no de entidades JPA).
- El estado de servidor vive en TanStack Query, no en un store global.
- ADMIN no usa rutas ni hooks de carrito/checkout/assistant/favoritos: el backend las deniega (`hasRole(CUSTOMER)`).

---

## 3. Estructura de carpetas

**DECIDIDO:** raíz de la app Next.js = `frontend/` (sin `src/`), con `app/`, `features/` y `shared/` al mismo nivel. Alias `@/*` → `frontend/*`.

```text
frontend/
├── app/
│   ├── globals.css                # @import "tailwindcss" + @theme
│   ├── layout.tsx                 # shell público
│   ├── page.tsx                   # home
│   ├── login/
│   ├── register/
│   ├── catalog/
│   ├── categories/[categoryId]/
│   ├── products/[productId]/
│   ├── search/
│   ├── (customer)/                # layout CUSTOMER
│   │   ├── cart/
│   │   ├── favorites/
│   │   ├── checkout/
│   │   ├── orders/
│   │   ├── lists/
│   │   ├── addresses/
│   │   └── assistant/
│   └── (admin)/                   # layout ADMIN
│       └── admin/
│           ├── page.tsx
│           ├── categories/
│           ├── products/
│           ├── orders/
│           ├── payments/[paymentId]/
│           └── knowledge/
├── features/
│   ├── catalog/
│   ├── auth/
│   ├── account/
│   ├── cart/
│   ├── favorites/
│   ├── lists/
│   ├── orders/
│   ├── assistant/
│   └── admin/
│       # cada feature: components/, hooks/, api.ts (wrappers del client)
├── shared/
│   ├── api/                       # client HTTP único, problem+json, tipos REST
│   ├── auth/                      # SessionProvider, guards de UI, header Bearer
│   ├── money/                     # Money { amount, currency: "COP" }
│   ├── errors/                    # ApiProblem → mensajes UI
│   ├── ui/                        # design system propio (Tailwind)
│   ├── config/                    # origen API (browser vs RSC)
│   └── utils/
├── tests/
├── public/
├── next.config.ts                 # rewrites /api/v1 → Spring
├── postcss.config.mjs             # @tailwindcss/postcss
├── tsconfig.json
├── package.json
└── pnpm-lock.yaml
```

Responsabilidades:

| Zona | Responsabilidad |
|---|---|
| `app/` | Routing y composición. Sin reglas de negocio. |
| `features/*/api.ts` | Funciones tipadas por endpoint (`listPublicProducts`, `checkout`). |
| `shared/api` | `request()`, headers, status, Problem Details. Sin reglas de negocio. |
| `shared/auth` | Guardar/limpiar sesión; **no** autorizar (eso es el JWT en el servidor). |
| `shared/ui` | Botones, inputs, cards, alerts, dialogs, tables (internos, no librería de UI). |
| `tests/` | Vitest / Testing Library / Playwright (**PROPUESTO** al implementar). |

Sin `tailwind.config.js`: Tailwind 4 es CSS-first.

---

## 4. Routing

**DECIDIDO:** rutas de UI ≠ rutas REST. La protección de UI es UX; la autoridad es Spring Security (`anyRequest().denyAll()`).

### 4.1 Público

| Ruta UI | Uso | API |
|---|---|---|
| `/` | Home / destacados | `GET /api/v1/categories`, `GET /api/v1/products` (sin `view` → PUBLIC) |
| `/login` | Login | `POST /api/v1/auth/login` |
| `/register` | Registro CUSTOMER | `POST /api/v1/customers` |
| `/catalog` | Listado completo | `GET /api/v1/products` (sin `page`/`size`; el storefront no envía `view`) |
| `/categories/[categoryId]` | Productos de categoría | `GET /api/v1/categories/{categoryId}`, `GET /api/v1/products?categoryId=` |
| `/products/[productId]` | Detalle | `GET /api/v1/products/{productId}` — no vendible → 404 |
| `/search?text=` | Búsqueda | `GET /api/v1/products/search?text=` (no existe parámetro `q`) |

### 4.2 CUSTOMER

| Ruta UI | API |
|---|---|
| `/cart` | `GET/POST/PUT/DELETE /api/v1/cart` |
| `/favorites` | `GET /api/v1/favorites`, `POST /api/v1/favorites/{productId}` (201 nuevo / 200 existente), `DELETE /api/v1/favorites/{productId}` (204) |
| `/checkout` | `GET /api/v1/addresses`, `GET /api/v1/cart`, `POST /api/v1/orders` |
| `/orders` | `GET /api/v1/orders?page&size` |
| `/orders/[orderId]` | `GET /api/v1/orders/{id}` (incluye `payment` para comprobante de UI), `POST .../cancel` |
| `/lists` | `GET/POST /api/v1/shopping-lists` |
| `/lists/[shoppingListId]` | `GET/PATCH /api/v1/shopping-lists/{id}`; `POST/PATCH/DELETE …/items`; `DELETE …/items` (clear) |
| `/addresses` | `/api/v1/addresses` |
| `/assistant` | `POST /api/v1/assistant/chat` |

No hay REST para “añadir lista completa al carrito” (no existe `POST /api/v1/shopping-lists/{id}/cart` ni `POST /cart/from-list`). Esa capacidad existe como use case/tool del Assistant (`AddShoppingListToCart`). La UI de listas añade ítems al carrito **uno a uno** con `POST /api/v1/cart/items`.
### 4.3 ADMIN

| Ruta UI | API |
|---|---|
| `/admin` | Hub de navegación. **No hay** endpoint de métricas. |
| `/admin/categories` | GET `view=ADMIN` + POST/PUT/activate/deactivate |
| `/admin/products` | GET `view=ADMIN` + create/update + activate/deactivate + `POST .../price` (ajuste de stock en el detalle) |
| `/admin/products/[productId]` | GET `view=ADMIN` + update de ficha; operaciones independientes: `ProductPricePanel` (`POST .../price`), `ProductStatusActions` (activate/deactivate), `ProductStockPanel` (`POST .../stock`) |
| `/admin/orders` | `GET /api/v1/admin/orders?page&size` (todos los estados). Ventas: mismos query params + `status=CONFIRMED,PREPARING,READY,DELIVERED` (también vale `status` repetido). |
| `/admin/orders/[orderId]` | `GET /api/v1/admin/orders/{orderId}` (incluye `payment` anidado); `POST /api/v1/orders/{orderId}/status` |
| `/admin/payments/[paymentId]` | `GET /api/v1/payments/{paymentId}` |
| `/admin/knowledge` | `/api/v1/knowledge/**` |

**DECIDIDO:** la tabla admin de pedidos usa `GET /api/v1/admin/orders`. Ventas es esa misma lista con filtro `status=CONFIRMED,PREPARING,READY,DELIVERED` (sin `PENDING` ni `CANCELLED`). No existe `/api/v1/admin/sales`. No reutilizar `GET /api/v1/orders` (CUSTOMER, propios). El detalle admin usa `GET /api/v1/admin/orders/{orderId}` (cualquier cliente). `GET /api/v1/orders/{orderId}` sigue siendo CUSTOMER con ownership (ajeno → 404 `ORDER_NOT_FOUND`). El comprobante de compra es una representación de UI del detalle de pedido enriquecido (`payment`); no hay endpoint `/receipt` ni URL pública.

### 4.4 Guards de UI

**DECIDIDO:**

- Grupo `(customer)`: si no hay sesión o `role !== CUSTOMER` → `/login`.
- Grupo `(admin)`: si `role !== ADMIN` → `/login` o home.
- ADMIN autenticado no ve carrito, checkout, assistant ni controles de favoritos (el API respondería `403 ACCESS_DENIED`).

El middleware de Next.js **no** puede leer `sessionStorage`. Un cookie de pista (`sf_role`) sería spoofable. **DECIDIDO:** guards de cliente + 401/403 del API. Cookie HttpOnly **no** forma parte del MVP: el backend no emite `Set-Cookie`.

---

## 5. Autenticación

Flujo real del backend (sin capacidades inventadas):

```text
POST /api/v1/auth/login { email, password }
  → 200 AuthenticationRestResponse { userId, role, accessToken, expiresAt }
  → requests: Authorization: Bearer <accessToken>
  → 401 UNAUTHENTICATED | 403 ACCESS_DENIED | 403 USER_INACTIVE
```

JWT (`superfercho.security.jwt.expiration: 15m`): claims `sub` = userId, `role` = `CUSTOMER` | `ADMIN`. Sin refresh, sin revocación, CSRF deshabilitado, sin cookies de sesión, sin `GET /me`.

Tras login no hay `fullName` ni `email` en el token. El registro sí devuelve esos campos, pero no el login.

### 5.1 Alternativas de almacenamiento del accessToken

| | A. Memoria | B. sessionStorage | C. localStorage | D. Cookie HttpOnly |
|---|---|---|---|---|
| Persistencia | Solo el tab en RAM | Por origen y pestaña | Por origen, indefinida (hasta borrar) | Por cookie; el JS no lee el valor |
| XSS | El token no está en Web Storage; un script inyectado igual puede leer variables si comparte el heap, pero no queda en disco | Un script inyectado puede leerlo y exfiltrarlo | Igual que B, más fácil de persistir el robo | El token no es accesible a JS; XSS no lo lee (sí puede abusar de cookies si el request las envía) |
| Recargar | Sesión perdida | Se restaura | Se restaura | Se restaura si la cookie vive |
| Cerrar navegador / pestaña | Perdida | Perdida al cerrar la pestaña | Sobrevive | Depende de Max-Age / Session cookie |
| Compatible con el backend actual | Sí: el body ya trae `accessToken`; el cliente pone `Authorization: Bearer` | Sí, mismo contrato | Sí, mismo contrato | **No.** El login devuelve JSON, no `Set-Cookie`. Spring espera header Bearer, no cookie de sesión |
| Cambio de backend | Ninguno | Ninguno | Ninguno | Sí: emitir cookie, dejar de (o además) devolver el JWT al JS, CORS/CSRF, SameSite. Fuera del alcance frontend-only |

**DECIDIDO (MVP):** alternativa **B. sessionStorage**, con copia en memoria para no parsear Storage en cada request.

Motivo: el backend entrega el token al JavaScript; A pierde la sesión al recargar (malo con TTL de 15 min y catálogo); C persiste tras cerrar el navegador sin beneficio (el JWT ya caduca a los 15 min); D exige cambiar Identity/Security. XSS sigue siendo el riesgo residual: TTL corto, no guardar password ni el secret JWT, CSP al implementar.

No hay refresh: a los ~15 min el usuario vuelve a login.

### 5.2 Sesión (conceptual)

**DECIDIDO:** un `SessionProvider` (React Context) en `shared/auth`. No mezclar el token con TanStack Query.

Estado de sesión:

```ts
type Session = {
  userId: string;
  role: "CUSTOMER" | "ADMIN";
  accessToken: string;
  expiresAt: string; // Instant ISO del backend
} | null;
```

Comportamiento:

| Evento | Efecto |
|---|---|
| Hidratar | Al montar, leer sessionStorage. Si `expiresAt` ≤ ahora → logout local |
| `login` | Guardar `AuthenticationRestResponse` (sin password) en memoria + sessionStorage |
| `logout` | Borrar Storage y estado; invalidar queries autenticadas |
| Request | API client lee el token de la sesión y pone `Authorization: Bearer` |
| Reloj ≥ `expiresAt` | Logout local **antes** de llamar; el 401 del API sigue siendo la autoridad |
| 401 `UNAUTHENTICATED` | Logout + redirigir a `/login` (excepto el propio login) |
| 401 `INVALID_CREDENTIALS` | No hay sesión que borrar; error en el form |
| 403 `ACCESS_DENIED` | Mantener sesión; pantalla de no autorizado |
| 403 `USER_INACTIVE` | Logout; mensaje de cuenta inactiva |

El menú CUSTOMER/ADMIN es UX. Spring `hasRole` autoriza.

Las llamadas autenticadas son **client-side**. Un Server Component no puede leer sessionStorage. El catálogo público sí puede pedirse en RSC contra Spring (origen interno, sin JWT).

### 5.3 Cliente autenticado

**DECIDIDO:** el API client añade `Authorization: Bearer` si hay token. Nunca envía `userId` / `customerId` en body o query para ownership.

---

## 6. API Client

**DECIDIDO:** un único módulo `frontend/shared/api/` sobre `fetch`. Sin reglas de negocio (no stock, no vendibilidad, no checkout).

Contiene:

- `request()` — GET, POST, PUT, PATCH, DELETE
- parseo de JSON 2xx a DTO
- `ApiProblem` — envoltorio interno (no es un DTO del backend)
- `Authorization: Bearer` si hay sesión
- `Idempotency-Key` solo en `POST /api/v1/orders`
- parseo de `application/problem+json` (RFC 7807 + property `code`)

Base de URL:

| Contexto | Base | Motivo |
|---|---|---|
| Navegador | `""` → `/api/v1/...` | same-origin; el rewrite de Next evita CORS |
| RSC / servidor Next | `http://localhost:8080` en desarrollo | `fetch` relativo no tiene origen en servidor; llamada directa a Spring, sin CORS |

No usar `NEXT_PUBLIC_*` para secretos. Un `NEXT_PUBLIC_API_BASE_URL` vacío es el default del browser.

**DECIDIDO** forma de error interno:

```ts
type ApiProblem = {
  status: number;
  code?: string;
  title?: string;
  detail?: string;
};
```

RFC 7807 de Spring incluye `type`, `title`, `status`, `detail`, `instance` y la property `code` que ya definen los handlers.

Mapeo UI (centralizado, no por componente):

| HTTP | Uso típico |
|---|---|
| 2xx | parsear DTO |
| 400 | validación; mostrar `detail` / `code` en el form |
| 401 | sesión |
| 403 | rol o usuario inactivo |
| 404 | recurso no vendible / no propio / inexistente |
| 409 | conflicto de negocio (precio, stock, idempotencia, barcode, etc.) |
| 500 | `INTERNAL_ERROR`; mensaje genérico, sin stack |

**DECIDIDO:** el frontend no habla con PostgreSQL, Flyway ni JPA.

### 6.1 Desarrollo local

**DECIDIDO:**

| Proceso | Origen |
|---|---|
| Frontend Next.js | `http://localhost:3000` |
| Backend Spring Boot | `http://localhost:8080` |

Comunicación del navegador:

```text
Browser → http://localhost:3000/api/v1/...
       → rewrite Next.js
       → http://localhost:8080/api/v1/...
```

Configuración prevista (`frontend/next.config.ts`, no crear ahora):

```ts
rewrites() {
  return [
    {
      source: "/api/v1/:path*",
      destination: "http://localhost:8080/api/v1/:path*",
    },
  ];
}
```

El navegador no llama a `:8080`. No se necesita CORS en desarrollo.

### 6.2 Tailwind CSS 4 (previsto, no creado)

Según la guía oficial [Install Tailwind CSS with Next.js](https://tailwindcss.com/docs/installation/framework-guides/nextjs):

1. Dependencias: `tailwindcss`, `@tailwindcss/postcss`, `postcss`.
2. `postcss.config.mjs` con plugin `"@tailwindcss/postcss": {}`.
3. `app/globals.css`: `@import "tailwindcss";` y tokens en `@theme { ... }`.
4. Importar `globals.css` en `app/layout.tsx`.

No hay `tailwind.config.js`. No `@tailwind base/components/utilities` (eso es v3). No Material UI, Chakra, Ant Design, Bootstrap ni shadcn/ui. El sistema visual se construye en `shared/ui` con utilidades Tailwind.

---

## 7. Contratos REST (inventario real)

`Money` JSON: `{ "amount": 10.50, "currency": "COP" }` (record Java `Money(BigDecimal amount, String currency)`; Jackson serializa `amount` como número). Enums: nombres Java (`ACTIVE`, `SIMULATED_CARD`, …).

### 7.1 Auth (público)

| Método | Path | Auth | Request | Response | Errores |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | none | `AuthenticateUserRequest`: `email`, `password` | `AuthenticationRestResponse`: `userId`, `role`, `accessToken`, `expiresAt` | 401 `INVALID_CREDENTIALS`, 403 `USER_INACTIVE` |

### 7.2 Customers (público)

| Método | Path | Request | Response | Errores |
|---|---|---|---|---|
| POST | `/api/v1/customers` | `RegisterCustomerRequest`: `documentType`, `documentNumber`, `fullName`, `email`, `phone`, `password` | 201 `RegisteredCustomerRestResponse`: `id`, `documentType`, `documentNumber`, `fullName`, `email`, `phone`, `role`, `status`, `createdAt` | 400 `INVALID_REGISTRATION` / `INVALID_USER`, 409 `USER_ALREADY_EXISTS` / `DOCUMENT_ALREADY_EXISTS` |

No hay GET perfil.

### 7.3 Addresses (`CUSTOMER`)

| Método | Path | Request | Response |
|---|---|---|---|
| GET | `/api/v1/addresses` | — | `AddressRestResponse[]` |
| POST | `/api/v1/addresses` | `AddAddressRequest`: `label`, `recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`, `isDefault` | 201 `AddressRestResponse` |
| PUT | `/api/v1/addresses/{addressId}` | `UpdateAddressRequest` (sin `isDefault`) | `AddressRestResponse` |
| DELETE | `/api/v1/addresses/{addressId}` | — | desactiva; no borra fila |
| POST | `/api/v1/addresses/{addressId}/default` | — | `AddressRestResponse` |

`AddressRestResponse`: `id`, `label`, `recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`, `isDefault`, `status` (`ACTIVE`/`INACTIVE`), `createdAt`, `updatedAt`.

Errores: 404 `ADDRESS_NOT_FOUND`, 400 `INVALID_ADDRESS`, 409 `DUPLICATE_DEFAULT_ADDRESS` / `ADDRESS_INACTIVE`.

### 7.4 Catalog

GET sin `view` o `view=PUBLIC`: permitAll. `view=ADMIN`: JWT ADMIN. El storefront usa el comportamiento por defecto (PUBLIC) y **no envía** `view`.

`CategoryRestResponse`: `id`, `name`, `description`, `status`, `createdAt`, `updatedAt`.

`ProductRestResponse`: `id`, `categoryId`, `barcode`, `name`, `brand`, `description`, `price`, `stock`, `imageUrl`, `status`, `createdAt`, `updatedAt`.

| Método | Path | Auth | Request / query | Response |
|---|---|---|---|---|
| GET | `/api/v1/categories` | public / ADMIN | `view?` | lista completa |
| GET | `/api/v1/categories/{categoryId}` | public / ADMIN | `view?` | una; pública INACTIVE → 404 |
| POST | `/api/v1/categories` | ADMIN | `CreateCategoryRequest`: `name`, `description` | 201 |
| PUT | `/api/v1/categories/{categoryId}` | ADMIN | `UpdateCategoryRequest`: `name`, `description` | |
| POST | `.../activate` `.../deactivate` | ADMIN | — | |
| GET | `/api/v1/products` | public / ADMIN | `categoryId?` (UUID); `status?` y `view?` solo ADMIN | lista **completa, sin paginación** (no hay `page` ni `size`) |
| GET | `/api/v1/products/search` | public / ADMIN | `text?`; `view?` solo ADMIN | lista completa; busca en `name`, `brand` y `barcode`; `text` blank → `[]` |
| GET | `/api/v1/products/{productId}` | public / ADMIN | `view?` | pública no vendible → 404 |
| POST | `/api/v1/products` | ADMIN | `CreateProductRequest`: `categoryId`, `barcode`, `name`, `brand`, `description`, `price`, `stock`, `imageUrl` | 201 |
| PUT | `/api/v1/products/{productId}` | ADMIN | `UpdateProductRequest`: `categoryId`, `barcode`, `name`, `brand`, `description`, `imageUrl` (no precio ni stock aquí) | |
| POST | `.../activate` `.../deactivate` | ADMIN | — | |
| POST | `.../price` | ADMIN | `ChangeProductPriceRequest`: `price` | |
| POST | `.../stock` | ADMIN | `AdjustProductStockRequest`: `{ "stock": <int >= 0> }` (valor absoluto; el cliente **no** envía `expectedStock`; el CAS lo hace el backend). Respuesta `ProductRestResponse`. Path bajo `/api/v1/products/…` (no `/admin/products` ni `/admin/stock`). | |

Stock Admin (contrato real + UI):

- stock inicial en `POST /products`;
- ajuste administrativo posterior con `POST /products/{productId}/stock` (absoluto, no delta; no reserva de checkout);
- el formulario de edición de ficha (`PUT`) no modifica stock ni precio;
- `ProductStockPanel` en `/admin/products/[productId]` es la UI de ese endpoint (panel separado del form de ficha, de `ProductPricePanel` y de `ProductStatusActions`);
- validación de cliente: entero `>= 0`; éxito → mensaje local + invalidación de producto admin; error → Problem Details (`404 PRODUCT_NOT_FOUND`, `409 PRODUCT_STOCK_CONFLICT`, `400 INVALID_PRODUCT`);
- checkout descuenta y cancelación restaura vía `InventoryPort` en el backend; el frontend Admin no llama esos flujos;
- Orders no usa el endpoint administrativo de stock;
- no hay historial de movimientos, razón obligatoria, ledger, lotes, proveedores ni grilla de inventario;
- no es inventario avanzado.

Errores: 400 `INVALID_CATEGORY` / `INVALID_PRODUCT` / `INVALID_CATEGORY_REFERENCE`, 404 `CATEGORY_NOT_FOUND` / `PRODUCT_NOT_FOUND`, 409 `DUPLICATE_BARCODE` / `PRODUCT_STOCK_CONFLICT` (ajuste concurrente de stock).

Auth de mutaciones Admin de producto (incluye `…/stock`): `ADMIN` permitido; `CUSTOMER` → 403; no autenticado → 401; resto → `denyAll` del backend.

Contrato del **storefront** (descubierto en la implementación; no inventar lo que no está en el controller):

- `GET /products` y `GET /products/search` devuelven listas completas. **No existe paginación** de catálogo (`page`, `size`, máximo 100 no aplican aquí; esos query params son de pedidos admin, no de productos).
- El único filtro de listado público es `categoryId` (UUID). No existen filtros `category`, `availability` ni `brand` como query params independientes.
- `brand` no se filtra aparte: participa en la búsqueda textual `GET /products/search?text=`.
- El parámetro de búsqueda es `text`, no `q`. La ruta UI es `/search?text=`.
- Vista pública por defecto: el storefront no envía `view`. El backend excluye productos y categorías INACTIVE de esa vista; el frontend no vuelve a filtrarlos.

Vendibilidad (backend): `Product.status == ACTIVE` **y** `Category.status == ACTIVE`. El público no lista ni vende el resto. El frontend no reimplementa la regla: un 404 público es “no disponible”.

### 7.5 Shopping (`CUSTOMER`)

El carrito es del CUSTOMER autenticado. El cliente **no envía** `customerId` ni `userId`: el backend toma la identidad del JWT.

`CartRestResponse`: `id`, `customerId`, `status` (`ACTIVE`), `items[]`, `createdAt`, `updatedAt`.

`CartItemRestResponse`: `id`, `productId`, `quantity`, `priceAtAddition`, `addedAt`, `updatedAt`.

El REST del carrito **no** incluye nombre, imagen, stock, subtotal ni total.

**Precio.** `priceAtAddition` es el precio de catálogo vigente **en el momento de crear la línea** (alta). Un `PUT` de cantidad **no** lo recalcula. Es **informativo**: no es el precio garantizado de compra. Checkout valida el precio **vigente** (`expectedUnitPrice` vs catálogo).

**Stock.** El carrito **no reserva inventario**. Shopping **no** rechaza `stock = 0` ni cantidades por encima del stock disponible. La disponibilidad para comprar se valida en checkout.

| Método | Path | Comportamiento real |
|---|---|---|
| GET | `/api/v1/cart` | Obtiene o crea el carrito `ACTIVE` del CUSTOMER autenticado. No recibe `customerId`. |
| POST | `/api/v1/cart/items` | `AddItemRequest`: `productId`, `quantity` > 0. El producto debe ser vendible (producto **y** categoría `ACTIVE`). No reserva stock. No rechaza `stock = 0`. |
| PUT | `/api/v1/cart/items/{productId}` | `ChangeItemQuantityRequest`: `quantity` > 0. No valida stock. |
| DELETE | `/api/v1/cart/items/{productId}` | Elimina la línea. |
| DELETE | `/api/v1/cart` | Vacía el carrito. |

Producto inexistente o no vendible → 404 `PRODUCT_NOT_FOUND`. Cantidad ≤ 0 en dominio → 400 `INVALID_CART_ITEM`. Producto que no está en el carrito → 400 `INVALID_CART`.

### 7.5.1 Favorites (`CUSTOMER`)

Favoritos es del CUSTOMER autenticado. El cliente **no envía** `customerId` ni `userId`: el backend toma la identidad del JWT. Query key única: `['favorites']`. La membresía de un producto se deriva de `favorite.productId === product.id` sobre esa query; no hay store paralelo, `localStorage` ni pending-intent.

`GET /api/v1/favorites` → `{ items: [{ productId, createdAt, product }] }`. El backend ya compone `product`; el frontend **no** hace N+1 obligatorio contra `/products/{id}` para pintar la ficha de favorito.

`product` puede ser `null` (el id ya no existe en Catalog). Si el producto existe pero no es vendible, el card trae `available = false`. Eso **no** implica necesariamente `status = INACTIVE`: un producto `ACTIVE` con categoría `INACTIVE` también es `available = false`. El frontend **no** elimina automáticamente esos favoritos.

`FavoriteProduct.available` es vendibilidad de catálogo (producto y categoría ACTIVE). **No** es stock. `isProductAvailable(product)` del catálogo sigue significando `stock > 0` para compra. Un producto ACTIVE con stock 0 puede ser favorito.

Stock en la UI de `/favorites`: el payload de favoritos **no** incluye `stock`. La página hidrata stock mediante el catálogo público (`useProductsQuery` / `GET /products`), el mismo patrón que listas. Sellability (`available`) y stock se muestran por separado. El CTA “Agregar al carrito” usa `canOfferAddToCart(sellable, stock)`: se oculta cuando el stock conocido es `<= 0`; no se reinterpreta `available=false` como agotado. Si el stock aún no está en el listado público, el CTA sigue la sellability sin inventar inventario.
| Método | Path | Comportamiento real |
|---|---|---|
| GET | `/api/v1/favorites` | Lista del CUSTOMER autenticado, en el orden del backend. |
| POST | `/api/v1/favorites/{productId}` | Sin body. 201 si es nuevo, 200 si ya existía. Body `{ id, productId, createdAt }`. El frontend no distingue 200/201 para actualizar estado: invalida `['favorites']`. |
| DELETE | `/api/v1/favorites/{productId}` | Sin body. Siempre 204, aunque el favorito no exista. Invalida `['favorites']`. |

Producto inexistente o no vendible al **añadir** → 404 `PRODUCT_NOT_FOUND`. Sin JWT → 401 `UNAUTHENTICATED`. ADMIN → 403 `ACCESS_DENIED`.

**GUEST:** no ejecuta POST/DELETE. Redirige a `/login?next=<ruta-actual>` (incluye query string; p. ej. `/search?text=leche`). Tras el login vuelve a la ruta y debe pulsar de nuevo el corazón.

**ADMIN:** no renderiza `FavoriteToggle` ni enlaces a `/favorites`.

### 7.6 Shopping lists (`CUSTOMER`)

Rutas UI: `/lists`, `/lists/[shoppingListId]`.

`ShoppingListRestResponse`: `id`, `customerId`, `name`, `items[]`, `createdAt`, `updatedAt`.

`ShoppingListItemRestResponse`: `id`, `productId`, `quantity`, `createdAt` (el persistente también tiene `item_index`; no es campo de UI).

| Método | Path | Request |
|---|---|---|
| GET | `/api/v1/shopping-lists` | |
| POST | `/api/v1/shopping-lists` | `CreateShoppingListRequest`: `name` (obligatorio, ≤ 255) |
| GET | `/api/v1/shopping-lists/{id}` | 404 si no es del principal |
| PATCH | `/api/v1/shopping-lists/{id}` | `RenameShoppingListRequest`: `name` |
| POST | `/api/v1/shopping-lists/{id}/items` | `AddItemRequest`: `productId`, `quantity` |
| PATCH | `.../items/{productId}` | `ChangeItemQuantityRequest` |
| DELETE | `.../items/{productId}` | |
| DELETE | `.../items` | vacía ítems |

No hay DELETE de la lista completa. No hay REST “lista → carrito”.

Comportamiento UI / stock:

- la lista **no reserva** stock; `stock = 0` no impide mantener el producto en la lista;
- el detalle hidrata productos del catálogo público para nombre, precio y stock;
- sellability (`status === ACTIVE` en la proyección de catálogo usada) y stock se muestran por separado;
- “Agregar al carrito” es ítem a ítem (`POST /api/v1/cart/items` con la `quantity` de la línea); se oculta cuando el stock conocido es `<= 0`;
- el checkout sigue siendo la autoridad final de stock; añadir al carrito desde la lista no decrementa inventario.
### 7.7 Orders

Checkout **CUSTOMER autenticado**. El body **no** incluye `customerId` ni `userId`. ADMIN → `403 ACCESS_DENIED`. Sin JWT → `401 UNAUTHENTICATED`.

Header `Idempotency-Key`: Spring `required = false`; Application **exige** string no vacío. El backend **no** genera la key y **no** exige formato UUID (cualquier string no vacío). Scope: `customerId` + key. Fingerprint: `addressId|paymentMethod` + líneas ordenadas por `productId` (`productId:quantity:amount:currency`). Misma key + mismo fingerprint (no expirada) → **replay** del resultado (HTTP **201** otra vez). Misma key + fingerprint distinto → `409 IDEMPOTENCY_CONFLICT`. Retención **24 h**.

Request `CheckoutRequest` (únicos campos):

```json
{
  "addressId": "UUID",
  "paymentMethod": "SIMULATED_CARD | CASH_ON_DELIVERY",
  "items": [
    {
      "productId": "UUID",
      "quantity": 1,
      "expectedUnitPrice": { "amount": 10.50, "currency": "COP" }
    }
  ]
}
```

`items` debe coincidir exactamente con el carrito **ACTIVE**: mismos `productId`, mismas `quantity`, sin duplicados.

`expectedUnitPrice` es el precio **vigente** de catálogo (`GET /products/{id}`), **no** `priceAtAddition`. El backend lo compara con `Product.currentPrice()`. Diferencia → `409 PRODUCT_PRICE_CHANGED`. El Order persiste el precio vigente.

Stock: el carrito **no** reserva inventario. Checkout valida disponibilidad y decrementa de forma atómica (`UPDATE … AND stock >= :quantity`). `stock = 0` → `409 PRODUCT_NOT_AVAILABLE`. `quantity > stock` → `409 STOCK_UNAVAILABLE`. Una falla hace **rollback** de toda la transacción.

Dirección: solo `addressId`. Debe ser del CUSTOMER y `ACTIVE`. Si no es utilizable (inexistente, de otro cliente o inactiva) → `409 ADDRESS_NOT_AVAILABLE`. **No** es `ADDRESS_NOT_FOUND` (ese código es del CRUD de Identity). Snapshot congelado: `recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`. Sin `label` ni `isDefault`.

Pago (sin PAN/CVV):

| Método | Payment | `providerReference` | Order al crear |
|---|---|---|---|
| `SIMULATED_CARD` | `APPROVED` | `sim-approved` | `PENDING` |
| `CASH_ON_DELIVERY` | `PENDING` | `cod-pending` | `PENDING` |

El simulador no produce `DECLINED`; el código `PAYMENT_DECLINED` igual existe.

Checkout es **una** TX local PostgreSQL: idempotency → carrito → dirección → productos/precios → pago → stock → order → clear cart → idempotency result. El frontend no reproduce esa transacción: un POST y representar 201 o el Problem.

Éxito: **201 Created**, `Location: /api/v1/orders/{orderId}`. Body corto `CheckoutRestResponse` (**no** trae `items`, `shippingAddress` ni objeto `payment`):

```json
{
  "orderId": "UUID",
  "orderNumber": "ORD-...",
  "status": "PENDING",
  "paymentStatus": "APPROVED | PENDING",
  "total": { "amount": 21.00, "currency": "COP" }
}
```

El detalle completo es `GET /api/v1/orders/{orderId}`.

`OrderRestResponse`: `id`, `orderNumber`, `customerId`, `status`, `items[]`, `subtotal`, `total`, `shippingAddress`, `paymentId`, `createdAt`, `confirmedAt`, `cancelledAt`, `updatedAt`, `payment` (nullable).

`payment` (detalle; no se hidrata en listados): `paymentId`, `amount`, `paymentMethod`, `status` (`PENDING`/`APPROVED`/`DECLINED`), `providerReference`, `refundedAt`, `createdAt`, `updatedAt`. Reembolso = `APPROVED` + `refundedAt`. COD puede seguir `PENDING` en un pedido `DELIVERED`. `paymentId` ausente → `payment: null` y HTTP 200.

`OrderItemRestResponse`: `id`, `productId`, `productName`, `unitPrice`, `quantity`, `subtotal`.

`ShippingAddressRestResponse`: snapshot (`recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`).

`PagedOrdersRestResponse`: `items`, `page`, `size`, `totalElements`. Query `page` default 0, `size` default 20, máximo 100 (Application). `GET /api/v1/admin/orders` acepta `status` repetido o separado por comas. Sin `status` = todos los estados. Ventas = `CONFIRMED,PREPARING,READY,DELIVERED`.

| Método | Path | Rol |
|---|---|---|
| POST | `/api/v1/orders` | CUSTOMER; 201 + `Location: /api/v1/orders/{orderId}` |
| GET | `/api/v1/orders` | CUSTOMER (propios) |
| GET | `/api/v1/orders/{orderId}` | CUSTOMER; ajeno → 404 `ORDER_NOT_FOUND`; `payment` anidado para el comprobante de UI |
| POST | `/api/v1/orders/{orderId}/cancel` | CUSTOMER propietario; solo `PENDING` + 15 min desde `createdAt`; restaura stock; pago `APPROVED` → `refundedAt` (status sigue `APPROVED`); COD `PENDING` sin reembolso |
| POST | `/api/v1/orders/{orderId}/status` | ADMIN; body `UpdateOrderStatusRequest`: `status` adyacente. No cancela. |
| GET | `/api/v1/admin/orders` | ADMIN; paginado (`page`/`size`); `status` opcional (Ventas: `CONFIRMED,PREPARING,READY,DELIVERED`) |
| GET | `/api/v1/admin/orders/{orderId}` | ADMIN; cualquier cliente; `payment` anidado; inexistente → 404 `ORDER_NOT_FOUND` |

Errores de **checkout** (no usar `PRODUCT_NOT_FOUND` ni `ADDRESS_NOT_FOUND` aquí):

| Código | HTTP |
|---|---|
| `INVALID_CHECKOUT` | 400 |
| `INVALID_ORDER` | 400 |
| `UNAUTHENTICATED` | 401 |
| `ACCESS_DENIED` | 403 |
| `IDEMPOTENCY_CONFLICT` | 409 |
| `PRODUCT_PRICE_CHANGED` | 409 |
| `STOCK_UNAVAILABLE` | 409 |
| `PRODUCT_NOT_AVAILABLE` | 409 |
| `CART_EMPTY` | 409 |
| `ADDRESS_NOT_AVAILABLE` | 409 |
| `PAYMENT_DECLINED` | 409 |

Cancel / transición (además): 409 `CANCELLATION_NOT_ALLOWED`, `INVALID_ORDER_TRANSITION`; 400 `INVALID_ORDER_STATUS_UPDATE`.

Estados Order: `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`.

### 7.8 Payments (`ADMIN`)

| Método | Path | Response |
|---|---|---|
| GET | `/api/v1/payments/{paymentId}` | `PaymentRestResponse`: `id`, `orderId`, `amount`, `paymentMethod`, `status`, `providerReference`, `createdAt`, `updatedAt`, `refundedAt` |

No hay listado. `REFUNDED` no es un status; hay `refundedAt`.

### 7.9 Knowledge (`ADMIN`)

`DocumentRestResponse`: `id`, `title`, `source`, `content`, `status` (`RECEIVED`,`CHUNKED`,`READY`,`FAILED`,`INACTIVE`), `chunks[]`, `createdAt`, `updatedAt`.

`ChunkRestResponse`: `id`, `position`, `text`, `embedded`.

| Método | Path | Request |
|---|---|---|
| POST | `/api/v1/knowledge/documents` | `CreateDocumentRequest`: `title`, `source`, `content` |
| GET | `/api/v1/knowledge/documents` | lista |
| GET | `/api/v1/knowledge/documents/{id}` | |
| PUT | `.../content` | `ReplaceDocumentContentRequest`: `content` |
| POST | `.../process` | chunk + embed |
| POST | `.../deactivate` `.../reactivate` | |
| GET | `/api/v1/knowledge/search` | query `query`, `limit` (limit obligatorio en el controller) |

Errores: 400 `INVALID_DOCUMENT` / `INVALID_SEARCH_REQUEST`, 404 `DOCUMENT_NOT_FOUND`, 409 `KNOWLEDGE_PROCESSING_FAILED`.

El Assistant **no** usa este HTTP; usa `SearchKnowledgeUseCase` en servidor.

### 7.10 Assistant (`CUSTOMER`)

`POST /api/v1/assistant/chat`

Request `ChatRequest`: `conversationId` (UUID, opcional en la primera), `message`, `confirmation` opcional `{ "token": "..." }`.

**No** hay campo `userId`.

Response `ChatRestResponse`: `conversationId`, `assistantMessage`, `awaitingConfirmation`, `confirmationToken`, `confirmationType` (`CHECKOUT` | `CANCEL_ORDER` | null).

Errores: 400 `INVALID_CHAT_REQUEST` / `INVALID_CONFIRMATION` / `INVALID_TOOL_ARGUMENTS` / `TOOL_NOT_ALLOWED`, 404 `CONVERSATION_NOT_FOUND`, 409 `LLM_PROVIDER_FAILED`.

Conversaciones y tokens viven en memoria de proceso: se pierden al reiniciar el backend.

---

## 8. Estado

### Server state (red)

Productos, categorías, carrito, favoritos, listas, pedidos, direcciones, documentos Knowledge, mensajes de Assistant.

**DECIDIDO:** TanStack Query (`@tanstack/react-query` 5.x). Cache e invalidación tras mutar carrito/checkout/favoritos. **Rechazado:** Redux para este estado.

Invalidaciones mínimas: add-to-cart → `['cart']`; POST/DELETE favorites → `['favorites']`; checkout 201 → `['cart']` + `['orders']`; activate product → `['products']`.

### Client/UI state

Modales, filtros, texto de búsqueda, pasos de checkout, scroll del chat. Local a la página o `useState`.

### Session

`userId`, `role`, `accessToken`, `expiresAt` en `shared/auth` (`SessionProvider` + sessionStorage). No mezclar con TanStack Query.

---

## 9. Formularios y validación

**DECIDIDO:** validación de cliente = UX. El backend sigue siendo la autoridad.

**PROPUESTO:** schemas Zod espejo de los records REST (campos y tipos, no invariantes de stock).

| Form | Campos alineados al REST | 409 / errores |
|---|---|---|
| Login | `email`, `password` | `INVALID_CREDENTIALS` |
| Registro | los 6 de `RegisterCustomerRequest` | `USER_ALREADY_EXISTS`, `DOCUMENT_ALREADY_EXISTS` |
| Dirección | `AddAddressRequest` / `UpdateAddressRequest` | `DUPLICATE_DEFAULT_ADDRESS` |
| Lista | `name` | 400 `INVALID_SHOPPING_LIST` |
| Checkout | dirección, método, ítems del carrito + `expectedUnitPrice` vigente | ver §7.7 |
| Admin categoría | `name`, `description` | |
| Admin producto | create vs update vs price vs stock (requests distintos) | `DUPLICATE_BARCODE`, `INVALID_CATEGORY_REFERENCE`, `PRODUCT_STOCK_CONFLICT` |
| Admin knowledge | `title`, `source`, `content` | |

Presentación: `ApiProblem.code` + `detail` en `shared/errors`, no if/else en cada form.

---

## 10. Catálogo y comercio

**DECIDIDO:** el storefront solo usa vista PUBLIC. No envía `view` (el default del backend ya es PUBLIC). No envía `view=ADMIN`.

Consultas públicas reales:

| UI | API |
|---|---|
| Categorías | `GET /api/v1/categories` |
| Categoría | `GET /api/v1/categories/{categoryId}` |
| Listado | `GET /api/v1/products` — lista completa; filtro opcional `categoryId` |
| Búsqueda | `GET /api/v1/products/search?text=` — lista completa; `text` cubre nombre, marca y código |
| Detalle | `GET /api/v1/products/{productId}` |

No hay `page`/`size` en catálogo. No hay filtros `category`, `availability` ni `brand` independientes. `stock` viaja en el `ProductRestResponse`; la UI puede mostrar disponible/agotado, pero no existe query de disponibilidad.

Estados de UI:

| Situación | Cómo se refleja el backend |
|---|---|
| Loading / empty / error | query + listas vacías reales (el backend ya entrega la lista completa; no hay paginación de catálogo) |
| `stock = 0` | producto visible si es vendible; la UI de catálogo / favoritos / listas puede ocultar el CTA de carrito. Shopping **no** impide el alta a carrito ni a lista. En checkout: `stock = 0` → `PRODUCT_NOT_AVAILABLE`; `quantity > stock` → `STOCK_UNAVAILABLE`. Un ACTIVE con stock 0 **puede** ser favorito o ítem de lista. |
| No vendible (producto o categoría INACTIVE) | el backend los excluye del listado público; detalle público → 404; el alta al carrito, lista o favoritos → 404 `PRODUCT_NOT_FOUND`; checkout → `PRODUCT_NOT_AVAILABLE`; el frontend no refiltra. Un favorito ya guardado puede devolver `product: null` o `available=false` (con `status` ACTIVE o INACTIVE según el caso); no se borra solo. |
| `FavoriteProduct.available` vs stock | `available` en Favoritos es vendibilidad, no inventario. No usar `isProductAvailable()` (stock del catálogo) para interpretarlo. Stock 0 es un estado UI aparte. |
| Precio en carrito vs catálogo | `priceAtAddition` es informativo (precio al crear la línea). Checkout envía `expectedUnitPrice` = precio **vigente** (`Product.currentPrice()`). No tratar `priceAtAddition` como precio de cobro. |
| 409 `PRODUCT_PRICE_CHANGED` | recargar producto/carrito; no cobrar el precio viejo |
| 409 `STOCK_UNAVAILABLE` / `PRODUCT_NOT_AVAILABLE` | códigos de **checkout**, no de Shopping; no reintentar el mismo body a ciegas |

`AddShoppingListToCart` no tiene REST: **fuera del contrato REST MVP**. La UI de listas añade ítem a ítem al carrito (`POST /api/v1/cart/items`) o el Assistant usa la capacidad Application. No hay botón que llame un endpoint inexistente. Ese flujo no decrementa stock; el checkout sí.
---

## 11. Checkout (flujo visual)

```text
Cart → Address → Summary → Payment method → Confirm → Result
```

Todo ocurre contra el carrito activo y `POST /api/v1/orders`.

**DECIDIDO:**

1. El cliente genera un `Idempotency-Key` (cualquier string no vacío; el backend no exige UUID y no la genera) por intento de pago y lo reutiliza en retries del **mismo** fingerprint. Replay → 201. Fingerprint distinto → `409 IDEMPOTENCY_CONFLICT`. Retención 24 h.
2. `items` = líneas del `GET /cart` con `expectedUnitPrice` = `GET /products/{id}` (precio vigente vs `Product.currentPrice()`). `priceAtAddition` no se cobra. Stock y precio de compra se validan en esta TX, no al escribir el carrito.
3. `SIMULATED_CARD` → Payment `APPROVED` (`sim-approved`), Order `PENDING`. `CASH_ON_DELIVERY` → Payment `PENDING` (`cod-pending`), Order `PENDING`.
4. 201 + `Location: /api/v1/orders/{orderId}` → ir a `/orders/{orderId}` (detalle completo por GET; el POST solo trae el DTO corto). El carrito queda vacío en servidor.
5. El simulador **no** produce `DECLINED`; la UI igual debe manejar 409 `PAYMENT_DECLINED`.

No hay wizard de tarjeta real (el API no acepta PAN/CVV).

---

## 12. Admin

Hub `/admin` + las pantallas con API real:

- Categorías y productos con `view=ADMIN` (incluye INACTIVE).
- Stock inicial en create; ajuste administrativo absoluto por `POST /api/v1/products/{productId}/stock` (`ProductStockPanel` en el detalle: validación `>= 0`, éxito/error locales, separado del form de ficha). Sin grilla de inventario, ledger, lotes ni proveedores.
- Precio por `POST .../price`, no por PUT de producto (`ProductPricePanel` en el detalle).
- Activate/deactivate vía `ProductStatusActions` (producto) / acciones equivalentes de categoría.
- Knowledge: crear, listar, contenido, process, deactivate/reactivate, search.
- Pedidos: listado/tabla vía `GET /api/v1/admin/orders` (véase §4.3; Ventas = filtro multi-status en la misma ruta); detalle con transición de estado (`POST .../status`).
- Pagos: consulta por `paymentId` (sin listado global).

No hay dashboard de KPIs, no hay upload de imágenes (solo `imageUrl` string), no hay bootstrap de ADMIN por API pública. No hay `/admin/sales`, receipt, inventario avanzado, ledger, lotes ni proveedores.

---

## 13. Assistant

**DECIDIDO:** una sola pantalla `/assistant` (CUSTOMER). Un `POST /api/v1/assistant/chat` por turno.

UI:

- Hilo local de `{ role: user | assistant, content }` más `assistantMessage` del response.
- Guardar `conversationId` de la primera respuesta y reenviarlo.
- Loading mientras el POST (el LLM puede tardar; timeout de backend de lectura 60s).
- Si `awaitingConfirmation`: diálogo con el `assistantMessage`; Confirmar reenvía `{ confirmation: { token } }`; Cancelar no llama tools (el backend no ejecuta sin token válido).
- `confirmationType` `CHECKOUT` | `CANCEL_ORDER` solo para copy; **no** ejecutar checkout REST en paralelo.
- Tras éxito, invalidar queries de carrito/pedidos porque el servidor ya mutó por tools.

El frontend **no** implementa allowlist, fingerprints ni ownership. Un 400 `TOOL_NOT_ALLOWED` / `INVALID_CONFIRMATION` se muestra como error.

Knowledge en el chat es interno al backend. Admin Knowledge es otra área.

---

## 14. Design system (inicial)

**DECIDIDO:** Tailwind CSS 4 + piezas internas en `shared/ui`. Sin librerías de componentes de terceros.

**PROPUESTO** (tokens, no componentes aún):

- Tipografía: sans geométrica; títulos 600–700; cuerpo 16px / 1.5.
- Espaciado: escala 4/8/12/16/24/32/48 (alineada a Tailwind).
- Color: fondo claro, acento verdura/retail; error/success/warning; contraste WCAG AA. Definir en `@theme`.
- Piezas: Button (primary/secondary/ghost/destructive), Input, Select, Card de producto, Badge de stock/estado, Alert de Problem Details, Dialog (confirmación Assistant y checkout), Table admin, Navbar (público / CUSTOMER / ADMIN), Skeleton, Empty state.

Tono: supermercado colombiano profesional, no dashboard genérico.

---

## 15. Responsive

Breakpoints **PROPUESTOS** (alineados a CSS comunes, no arbitrarios de marca):

| Nombre | Ancho | Uso |
|---|---|---|
| mobile | &lt; 768px | nav inferior o menú; 1–2 columnas de catálogo; checkout en una columna |
| tablet | 768–1023px | 2–3 columnas |
| desktop | ≥ 1024px | nav horizontal; 4 columnas; admin con tabla |

Principios: carrito y checkout apilados en móvil; tablas admin con scroll horizontal; chat a pantalla completa en móvil.

---

## 16. Accesibilidad (mínimo)

**DECIDIDO** como requisito de implementación:

- Labels asociados; errores con `aria-describedby` y el `code` no como único texto.
- Foco visible; diálogos con trap de foco y Escape.
- HTML semántico (`main`, `nav`, `h1` único por vista).
- Botones reales, no `div` clickeable.
- Contraste AA; no información solo por color (stock).
- El chat anuncia nuevos mensajes (`aria-live`).

---

## 17. Testing

Pirámide **PROPUESTA:**

| Nivel | Herramienta | Qué |
|---|---|---|
| Unit | Vitest | Money display, parse de `ApiProblem`, guards de rol |
| Component | Testing Library | cards, forms, dialog de confirmación |
| Integración | MSW + Testing Library | login, add-to-cart, checkout 409 precio/stock |
| E2E | Playwright | happy path CUSTOMER; admin categoría; assistant confirmación mockeada |

Prioridad: auth, catálogo 404 no vendible, carrito, checkout + `Idempotency-Key`, CUSTOMER vs ADMIN (403), assistant sin `userId`.

No tests contra PostgreSQL desde el frontend.

---

## 18. Configuración de entorno

Públicas (navegador):

- Ninguna obligatoria si el browser usa `/api/v1` same-origin.
- Opcional: `NEXT_PUBLIC_API_BASE_URL` vacío en desarrollo.

Solo servidor Next (no exponer al bundle):

- Origen interno de Spring para RSC, p. ej. `SUPERFERCHO_API_ORIGIN=http://localhost:8080` en desarrollo.

Nunca en el cliente: `SUPERFERCHO_JWT_SECRET`, `OPENAI_API_KEY`, passwords de DB.

**DECIDIDO:** el frontend no incrusta secretos del backend.

---

## 19. Deployment

**DECIDIDO (estrategia, no implementación):**

Tres procesos: Next.js + Spring Boot + PostgreSQL.

| Opción | Encaje con el backend actual (sin CORS) |
|---|---|
| Reverse proxy same-origin (`/` → Next, `/api/v1` → Spring) | Preferida en producción. El navegador ve un solo origen; Bearer sigue igual |
| Next.js como origen público + rewrite de producción a Spring interno | Aceptable si Next es el único hostname público |
| CORS en Spring (orígenes distintos) | Requiere cambio de backend. **No** para el MVP si hay proxy |

**PENDIENTE (infraestructura externa):** proveedor concreto (Vercel, VPS, etc.), dominio y TLS.

El frontend no despliega Flyway ni el JAR.

---

## 20. Decisiones, rechazos y riesgos

### DECIDIDO

- Stack: Next.js 16.3.x App Router, React 19.3.x, TypeScript 5.9.3, Tailwind CSS 4.3.x, TanStack Query 5.x, pnpm 12.
- Ubicación: `frontend/` en el mismo repo; layout objetivo `backend/` + `frontend/` + `docs/`; sin monorepo tooling.
- Arquitectura: App Router + features + shared.
- Routing UI ≠ REST; guards de cliente; autoridad = Spring.
- Server state = TanStack Query; UI state local; sin Redux.
- Styling = Tailwind CSS 4 + `shared/ui` propio.
- Desarrollo local: `:3000` → rewrite `/api/v1` → Spring `:8080`.
- JWT MVP = sessionStorage + memoria; Bearer header; sin cookie HttpOnly.
- API client único (`shared/api`): fetch, ApiProblem, Bearer, Idempotency-Key, problem+json.
- Storefront PUBLIC; admin `view=ADMIN`.
- Assistant solo `POST /chat`; confirmación con token del backend.
- ADMIN no shopper.
- Tabla admin de pedidos vía `GET /api/v1/admin/orders`; Ventas = filtro `status`; detalle vía `GET /api/v1/admin/orders/{orderId}`. No reutilizar `GET /api/v1/orders` para ADMIN. Comprobante = UI del detalle enriquecido; sin `/receipt`.
- Stock Admin: create con stock inicial; ajuste absoluto `POST /api/v1/products/{productId}/stock` + `ProductStockPanel` (validación, éxito/error; separado del form de ficha); PUT de ficha sin stock/precio; sin ledger/lotes/proveedores/grilla.
- Favoritos y listas implementados (CUSTOMER); stock 0 no quita el ítem; CTA de carrito respeta stock conocido sin confundirlo con `available`.
- Checkout UI `/checkout` → REST `POST /api/v1/orders` + `Idempotency-Key`; cancelación 15 min desde `createdAt`; payment MVP sin status `REFUNDED`.
- Sin duplicar vendibilidad, stock atómico de checkout ni InventoryPort en el cliente.

### PROPUESTO (mejoras no bloqueantes)

- Zod para schemas de form (espejo de records REST, no invariantes de stock).
- Ampliar coverage Playwright/MSW donde aún no exista.
- Tokens visuales concretos (`@theme`) y copy de marca.

### Rechazado

- Redux, Axios, GraphQL, tRPC, Prisma, WebSocket de chat.
- Material UI, Chakra, Ant Design, Bootstrap, shadcn/ui.
- Turborepo, Nx, `src/` directory, `tailwind.config.js` de v3.
- Cookie HttpOnly sin cambio de API, `GET /me` inventado, **REST lista→carrito** (no forma parte del contrato REST MVP; existe Application/Assistant), reutilizar `GET /api/v1/orders` para ADMIN, `POST /api/v1/checkout`.
- TypeScript 7 y Next 16.4 canary en el arranque.

### Riesgos

| Riesgo | Mitigación |
|---|---|
| Token en JS (XSS) | TTL 15 min; no `localStorage`; CSP al endurecer despliegue |
| Sin refresh | 401 → login; UX de sesión corta |
| Sin CORS | rewrite/proxy same-origin |
| Sin `GET /me` | UI con `role` + `userId`; nombre solo si se guardó al registrar (opcional, frágil) |
| Assistant in-memory | avisar que el hilo muere al reiniciar el API |
| Catálogo GET sin paginar | listas grandes; paginación es cambio de backend |
| Favoritos sin stock en el payload | hidratar stock desde catálogo público; si falta, no inventar |
| Admin orders | listado y detalle por APIs ADMIN; snapshots históricos del pedido |

### PENDIENTE / fuera del MVP REST

Solo lo que depende de infraestructura externa o de un cambio de backend aún no existente:

- Cookie HttpOnly, refresh token, CORS explícito, `GET /me` (requieren backend).
- Hosting, dominio y TLS de producción.
- Paginación del catálogo: **no existe hoy** (`GET /products` y `GET /products/search` son listas completas). Añadir `page`/`size` sería un cambio de backend, no de frontend.
- REST lista→carrito: **no** está pendiente de implementación frontend; **no** forma parte del contrato REST MVP (véase Rechazado).

---

## 21. Contradicciones / límites respecto al backend

No hay contradicción en contratos inventados: este blueprint se atiene a los controllers.

Límites que el frontend **no** debe tapar con APIs ficticias:

1. No hay `GET /api/v1/me`.
2. No hay REST de Sales, Invoice ni receipts; Ventas filtra `GET /api/v1/admin/orders`.
3. No hay REST para `AddShoppingListToCart` (ni `POST /api/v1/checkout`).
4. No hay CORS configurado.
5. No hay refresh token.
6. ADMIN no puede usar cart/checkout/assistant/favorites.
7. El simulador CARD no declina; el código `PAYMENT_DECLINED` igual existe.
8. Conversaciones Assistant no son durables.
9. Favoritos REST no incluyen `stock`; la UI lo obtiene del catálogo público cuando está disponible.

---

## 22. Veredicto

Documento de arquitectura de referencia del **frontend MVP implementado** en `frontend/`, alineado al backend cerrado. Catálogo, favoritos, listas, carrito, checkout (`POST /orders`), pedidos, Admin (incluido stock) y Assistant consumen los contratos reales.