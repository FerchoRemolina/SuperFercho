# SuperFercho Frontend Technical Architecture Blueprint

**Estado:** Arquitectura objetivo (no implementada)

**Tipo:** Diseño técnico aprobable

**Fuente de verdad del backend:** código REST, seguridad y `docs/architecture/backend-technical-blueprint.md`

**Inspección del repositorio:** 2026-09-19

**Cierre de decisiones técnicas:** 2026-09-19 (sin implementación)

Leyenda de decisiones:

| Etiqueta | Significado |
|---|---|
| **DECIDIDO** | Encaja con el backend actual y se recomienda como base de implementación |
| **PROPUESTO** | Recomendación técnica; se puede sustituir sin cambiar contratos REST |
| **PENDIENTE** | No se puede cerrar con la información o capacidades actuales del backend |

Este documento no describe un frontend existente. No se deben inventar endpoints, DTOs ni reglas de negocio.

---

## 0. Hallazgo de inspección

**DECIDIDO (hecho observado):** no existe frontend en el repositorio.

No hay `package.json`, no hay `src/` de UI, no hay archivos `.tsx` / `.jsx` / `.vue`, no hay carpeta `frontend/` ni `web/`. El artefacto Maven (`com.superfercho:superfercho`) vive en `backend/`, con REST en `/api/v1`.

**DECIDIDO (ubicación objetivo al inicializar):** el frontend vivirá en `frontend/` como aplicación independiente en el mismo repositorio. No monorepo (sin Turborepo, Nx ni workspaces npm). Layout objetivo:

```text
SuperFercho/
├── backend/      # módulo Maven actual; el traslado es mecánico al inicializar
├── frontend/     # Next.js (aún no creado)
├── docs/
└── README.md
```

El módulo Maven vive en `backend/`. Ese traslado no cambia contratos REST.

El frontend, cuando se implemente, será un cliente nuevo que consume ese API. No duplica Domain, no habla con PostgreSQL y no reimplementa vendibilidad, stock, checkout ni Assistant.

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

### 1.1 Versiones recomendadas (inspección 2026-09-19)

Fuentes: npm `latest` estable, docs de Next.js 16, Tailwind v4, Node.js Release, pnpm. **No se instala nada en esta tarea.** Los parches exactos se pinnean en `frontend/package.json` al crear el proyecto.

| Paquete | Versión | Motivo |
|---|---|---|
| Next.js | **16.3.5** | `latest` estable. No 16.4 canary. App Router, `engines.node >= 20.9.0`, peer `react@^19` |
| React | **19.3.0** | `latest` estable, peer de Next 16 |
| React DOM | **19.3.0** | misma línea que React |
| TypeScript | **5.9.3** | Último 5.9.x confirmado. Next 16 exige ≥ 5.1.0. `typescript@7` es `latest` en npm; el MVP no lo persigue |
| Tailwind CSS | **4.3.3** | `latest` de la línea 4 aprobada |
| `@tailwindcss/postcss` | **4.3.3** | plugin oficial v4, misma versión que `tailwindcss` |
| `@tanstack/react-query` | **5.103.1** | `latest` de la línea 5; peer React 18 o 19 |
| `@types/react` / `@types/react-dom` | **19.3.x** | alineados a React 19.3 |
| Node.js | **24.21.0** (Active LTS «Krypton») | LTS, no Current 26. Mínimo Next: 20.9 |
| pnpm | **12.4.2** | `latest` de la línea 12 |

Al inicializar: `packageManager: "pnpm@12.4.2"` y `engines.node: ">=20.9.0"`. Si `create-next-app` fija un parche 16.3.x / 19.3.x distinto el mismo día, se acepta ese parche dentro de la misma minor.

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
- ADMIN no usa rutas ni hooks de carrito/checkout/assistant: el backend las deniega (`hasRole(CUSTOMER)`).

---

## 3. Estructura de carpetas

No crear todavía. **DECIDIDO:** raíz de la app Next.js = `frontend/` (sin `src/`, para que `app/`, `features/` y `shared/` queden al mismo nivel). Alias `@/*` → `frontend/*`.

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
| `/catalog` | Listado | `GET /api/v1/products` |
| `/categories/[categoryId]` | Productos de categoría | `GET /api/v1/categories/{id}`, `GET /api/v1/products?categoryId=` |
| `/products/[productId]` | Detalle | `GET /api/v1/products/{id}` — no vendible → 404 |
| `/search?q=` | Búsqueda | `GET /api/v1/products/search?text=` |

### 4.2 CUSTOMER

| Ruta UI | API |
|---|---|
| `/cart` | `GET/POST/PUT/DELETE /api/v1/cart` |
| `/checkout` | `GET /api/v1/addresses`, `GET /api/v1/cart`, `POST /api/v1/orders` |
| `/orders` | `GET /api/v1/orders?page&size` |
| `/orders/[orderId]` | `GET /api/v1/orders/{id}` (incluye `payment` para comprobante de UI), `POST .../cancel` |
| `/lists` | `GET/POST /api/v1/shopping-lists` |
| `/lists/[shoppingListId]` | GET/PATCH/items |
| `/addresses` | `/api/v1/addresses` |
| `/assistant` | `POST /api/v1/assistant/chat` |

No hay REST para “añadir lista completa al carrito”. Esa capacidad existe como use case/tool del Assistant, no como endpoint. La UI de listas no debe inventar `POST /cart/from-list`.

### 4.3 ADMIN

| Ruta UI | API |
|---|---|
| `/admin` | Hub de navegación. **No hay** endpoint de métricas. |
| `/admin/categories` | GET `view=ADMIN` + POST/PUT/activate/deactivate |
| `/admin/products` | GET `view=ADMIN` + mutaciones + `POST .../price` |
| `/admin/products/[productId]` | GET `view=ADMIN` + update |
| `/admin/orders` | `GET /api/v1/admin/orders?page&size` (todos los estados). Ventas: mismos query params + `status=CONFIRMED,PREPARING,READY,DELIVERED` (también vale `status` repetido). |
| `/admin/orders/[orderId]` | `GET /api/v1/admin/orders/{orderId}` (incluye `payment` anidado); `POST /api/v1/orders/{orderId}/status` |
| `/admin/payments/[paymentId]` | `GET /api/v1/payments/{paymentId}` |
| `/admin/knowledge` | `/api/v1/knowledge/**` |

**DECIDIDO:** la tabla admin de pedidos usa `GET /api/v1/admin/orders`. Ventas es esa misma lista con filtro `status=CONFIRMED,PREPARING,READY,DELIVERED` (sin `PENDING` ni `CANCELLED`). No existe `/api/v1/admin/sales`. No reutilizar `GET /api/v1/orders` (CUSTOMER, propios). El detalle admin usa `GET /api/v1/admin/orders/{orderId}` (cualquier cliente). `GET /api/v1/orders/{orderId}` sigue siendo CUSTOMER con ownership (ajeno → 404 `ORDER_NOT_FOUND`). El comprobante de compra es una representación de UI del detalle de pedido enriquecido (`payment`); no hay endpoint `/receipt` ni URL pública.

### 4.4 Guards de UI

**DECIDIDO:**

- Grupo `(customer)`: si no hay sesión o `role !== CUSTOMER` → `/login`.
- Grupo `(admin)`: si `role !== ADMIN` → `/login` o home.
- ADMIN autenticado no ve carrito, checkout ni assistant (el API respondería `403 ACCESS_DENIED`).

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

GET sin `view` o `view=PUBLIC`: permitAll. `view=ADMIN`: JWT ADMIN.

`CategoryRestResponse`: `id`, `name`, `description`, `status`, `createdAt`, `updatedAt`.

`ProductRestResponse`: `id`, `categoryId`, `barcode`, `name`, `brand`, `description`, `price`, `stock`, `imageUrl`, `status`, `createdAt`, `updatedAt`.

| Método | Path | Auth | Request / query | Response |
|---|---|---|---|---|
| GET | `/api/v1/categories` | public / ADMIN | `view?` | lista |
| GET | `/api/v1/categories/{categoryId}` | public / ADMIN | `view?` | una; pública INACTIVE → 404 |
| POST | `/api/v1/categories` | ADMIN | `CreateCategoryRequest`: `name`, `description` | 201 |
| PUT | `/api/v1/categories/{categoryId}` | ADMIN | `UpdateCategoryRequest`: `name`, `description` | |
| POST | `.../activate` `.../deactivate` | ADMIN | — | |
| GET | `/api/v1/products` | public / ADMIN | `categoryId?`, `status?`, `view?` | lista **sin paginación** |
| GET | `/api/v1/products/search` | public / ADMIN | `text?`, `view?` | lista; `text` blank → `[]` |
| GET | `/api/v1/products/{productId}` | public / ADMIN | `view?` | pública no vendible → 404 |
| POST | `/api/v1/products` | ADMIN | `CreateProductRequest`: `categoryId`, `barcode`, `name`, `brand`, `description`, `price`, `stock`, `imageUrl` | 201 |
| PUT | `/api/v1/products/{productId}` | ADMIN | `UpdateProductRequest`: `categoryId`, `barcode`, `name`, `brand`, `description`, `imageUrl` (no precio ni stock aquí) | |
| POST | `.../activate` `.../deactivate` | ADMIN | — | |
| POST | `.../price` | ADMIN | `ChangeProductPriceRequest`: `price` | |

Errores: 400 `INVALID_CATEGORY` / `INVALID_PRODUCT` / `INVALID_CATEGORY_REFERENCE`, 404 `CATEGORY_NOT_FOUND` / `PRODUCT_NOT_FOUND`, 409 `DUPLICATE_BARCODE`.

Vendibilidad (backend): `Product.status == ACTIVE` **y** `Category.status == ACTIVE`. El público no lista ni vende el resto. El frontend no reimplementa la regla: un 404 público es “no disponible”.

### 7.5 Shopping (`CUSTOMER`)

`CartRestResponse`: `id`, `customerId`, `status` (`ACTIVE`), `items[]`, `createdAt`, `updatedAt`.

`CartItemRestResponse`: `id`, `productId`, `quantity`, `priceAtAddition`, `addedAt`, `updatedAt`.

`priceAtAddition` es informativo; el cobro usa precio vigente.

| Método | Path | Request |
|---|---|---|
| GET | `/api/v1/cart` | get-or-create |
| POST | `/api/v1/cart/items` | `AddItemRequest`: `productId`, `quantity` > 0 |
| PUT | `/api/v1/cart/items/{productId}` | `ChangeItemQuantityRequest`: `quantity` > 0 |
| DELETE | `/api/v1/cart/items/{productId}` | |
| DELETE | `/api/v1/cart` | vacía |

Producto no vendible → 404 `PRODUCT_NOT_FOUND`.

### 7.6 Shopping lists (`CUSTOMER`)

`ShoppingListRestResponse`: `id`, `customerId`, `name`, `items[]`, `createdAt`, `updatedAt`.

`ShoppingListItemRestResponse`: `id`, `productId`, `quantity`, `createdAt`.

| Método | Path | Request |
|---|---|---|
| GET | `/api/v1/shopping-lists` | |
| POST | `/api/v1/shopping-lists` | `CreateShoppingListRequest`: `name` (obligatorio, ≤ 255) |
| GET | `/api/v1/shopping-lists/{id}` | 404 si no es del principal |
| PATCH | `/api/v1/shopping-lists/{id}` | `RenameShoppingListRequest`: `name` |
| POST | `.../items` | `AddItemRequest` |
| PATCH | `.../items/{productId}` | `ChangeItemQuantityRequest` |
| DELETE | `.../items/{productId}` | |
| DELETE | `.../items` | vacía ítems |

No hay DELETE de la lista. No hay REST “lista → carrito”.

### 7.7 Orders

Checkout **CUSTOMER**. Header `Idempotency-Key` (Spring `required = false`; Application rechaza blank).

Request `CheckoutRequest`: `addressId`, `paymentMethod` (`SIMULATED_CARD` | `CASH_ON_DELIVERY`), `items[]` de `CheckoutItemRequest`: `productId`, `quantity`, `expectedUnitPrice`.

Los ítems deben coincidir exactamente con el carrito activo.

201 `CheckoutRestResponse`: `orderId`, `orderNumber`, `status`, `paymentStatus`, `total`.

`OrderRestResponse`: `id`, `orderNumber`, `customerId`, `status`, `items[]`, `subtotal`, `total`, `shippingAddress`, `paymentId`, `createdAt`, `confirmedAt`, `cancelledAt`, `updatedAt`, `payment` (nullable).

`payment` (detalle; no se hidrata en listados): `paymentId`, `amount`, `paymentMethod`, `status` (`PENDING`/`APPROVED`/`DECLINED`), `providerReference`, `refundedAt`, `createdAt`, `updatedAt`. Reembolso = `APPROVED` + `refundedAt`. COD puede seguir `PENDING` en un pedido `DELIVERED`. `paymentId` ausente → `payment: null` y HTTP 200.

`OrderItemRestResponse`: `id`, `productId`, `productName`, `unitPrice`, `quantity`, `subtotal`.

`ShippingAddressRestResponse`: snapshot (`recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`).

`PagedOrdersRestResponse`: `items`, `page`, `size`, `totalElements`. Query `page` default 0, `size` default 20, máximo 100 (Application). `GET /api/v1/admin/orders` acepta `status` repetido o separado por comas. Sin `status` = todos los estados. Ventas = `CONFIRMED,PREPARING,READY,DELIVERED`.

| Método | Path | Rol |
|---|---|---|
| POST | `/api/v1/orders` | CUSTOMER |
| GET | `/api/v1/orders` | CUSTOMER (propios) |
| GET | `/api/v1/orders/{orderId}` | CUSTOMER; ajeno → 404 `ORDER_NOT_FOUND`; `payment` anidado para el comprobante de UI |
| POST | `/api/v1/orders/{orderId}/cancel` | CUSTOMER; solo `PENDING` + 15 min desde `createdAt` |
| POST | `/api/v1/orders/{orderId}/status` | ADMIN; body `UpdateOrderStatusRequest`: `status` adyacente. No cancela. |
| GET | `/api/v1/admin/orders` | ADMIN; paginado (`page`/`size`); `status` opcional (Ventas: `CONFIRMED,PREPARING,READY,DELIVERED`) |
| GET | `/api/v1/admin/orders/{orderId}` | ADMIN; cualquier cliente; `payment` anidado; inexistente → 404 `ORDER_NOT_FOUND` |

Errores checkout/cancel: 400 `INVALID_CHECKOUT` / `INVALID_ORDER`, 409 `IDEMPOTENCY_CONFLICT`, `PRODUCT_PRICE_CHANGED`, `STOCK_UNAVAILABLE`, `PAYMENT_DECLINED`, `CART_EMPTY`, `ADDRESS_NOT_AVAILABLE`, `PRODUCT_NOT_AVAILABLE`, `CANCELLATION_NOT_ALLOWED`, `INVALID_ORDER_TRANSITION`, 400 `INVALID_ORDER_STATUS_UPDATE`.

Estados: `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`.

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

Productos, categorías, carrito, listas, pedidos, direcciones, documentos Knowledge, mensajes de Assistant.

**DECIDIDO:** TanStack Query (`@tanstack/react-query` 5.x). Cache e invalidación tras mutar carrito/checkout. **Rechazado:** Redux para este estado.

Invalidaciones mínimas: add-to-cart → `['cart']`; checkout 201 → `['cart']` + `['orders']`; activate product → `['products']`.

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
| Admin producto | create vs update vs price (tres requests distintos) | `DUPLICATE_BARCODE`, `INVALID_CATEGORY_REFERENCE` |
| Admin knowledge | `title`, `source`, `content` | |

Presentación: `ApiProblem.code` + `detail` en `shared/errors`, no if/else en cada form.

---

## 10. Catálogo y comercio

**DECIDIDO:** el storefront solo usa vista PUBLIC (sin `view=ADMIN`).

Estados de UI:

| Situación | Cómo se refleja el backend |
|---|---|
| Loading / empty / error | query + listas vacías reales (no hay paginación de catálogo) |
| `stock = 0` | producto visible si es vendible; CTA deshabilitado; no inventar “ocultar” |
| No vendible (producto o categoría INACTIVE) | 404 en detalle público; no aparece en listados públicos |
| Precio en carrito vs catálogo | mostrar ambos; checkout envía precio **vigente** como `expectedUnitPrice` |
| 409 `PRODUCT_PRICE_CHANGED` | recargar producto/carrito; no cobrar el precio viejo |
| 409 `STOCK_UNAVAILABLE` / `PRODUCT_NOT_AVAILABLE` | no reintentar el mismo body a ciegas |

`AddShoppingListToCart` no tiene REST: la UI de listas permite añadir ítem a ítem al carrito (`POST /cart/items`) o usar Assistant. No hay botón que llame un endpoint inexistente.

---

## 11. Checkout (flujo visual)

```text
Cart → Address → Summary → Payment method → Confirm → Result
```

Todo ocurre contra el carrito activo y `POST /api/v1/orders`.

**DECIDIDO:**

1. El cliente genera un `Idempotency-Key` (UUID) por intento de pago y lo reutiliza en retries del **mismo** fingerprint.
2. `items` = líneas del `GET /cart` con `expectedUnitPrice` = `GET /products/{id}` (precio vigente), no `priceAtAddition`.
3. `SIMULATED_CARD` → `paymentStatus` `APPROVED` en el simulador actual; `CASH_ON_DELIVERY` → `PENDING`.
4. 201 → ir a `/orders/{orderId}`. El carrito queda vacío en servidor.
5. El simulador **no** produce `DECLINED`; la UI igual debe manejar 409 `PAYMENT_DECLINED`.

No hay wizard de tarjeta real (el API no acepta PAN/CVV).

---

## 12. Admin

Hub `/admin` + las pantallas con API real:

- Categorías y productos con `view=ADMIN` (incluye INACTIVE).
- Precio por `POST .../price`, no por PUT de producto.
- Knowledge: crear, listar, contenido, process, deactivate/reactivate, search.
- Pedidos: formulario de transición con `orderId` conocido (`POST .../status`). Sin grilla global.
- Pagos: consulta por `paymentId`.

No hay dashboard de KPIs, no hay upload de imágenes (solo `imageUrl` string), no hay bootstrap de ADMIN por API pública.

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
- Sin duplicar vendibilidad, stock ni checkout.

### PROPUESTO (al implementar, no bloquea el diseño)

- Zod para schemas de form (espejo de records REST, no invariantes de stock).
- Vitest, Testing Library, Playwright, MSW.
- Tokens visuales concretos (`@theme`) y copy de marca.

### Rechazado

- Redux, Axios, GraphQL, tRPC, Prisma, WebSocket de chat.
- Material UI, Chakra, Ant Design, Bootstrap, shadcn/ui.
- Turborepo, Nx, `src/` directory, `tailwind.config.js` de v3.
- Cookie HttpOnly sin cambio de API, `GET /me` inventado, REST lista→carrito inventado, reutilizar `GET /api/v1/orders` para ADMIN.
- TypeScript 7 y Next 16.4 canary en el arranque.

### Riesgos

| Riesgo | Mitigación |
|---|---|
| Token en JS (XSS) | TTL 15 min; no `localStorage`; CSP al implementar |
| Sin refresh | 401 → login; UX de sesión corta |
| Sin CORS | rewrite/proxy same-origin |
| Sin `GET /me` | UI con `role` + `userId`; nombre solo si se guardó al registrar (opcional, frágil) |
| Assistant in-memory | avisar que el hilo muere al reiniciar el API |
| Catálogo GET sin paginar | listas grandes; paginación es cambio de backend |
| Admin orders | listado y detalle por APIs ADMIN; snapshots históricos del pedido |
| Traslado Maven → `backend/` | mecánico al inicializar; no mezclar con cambios de dominio |

### PENDIENTE

Solo lo que depende de implementación o de infraestructura / backend futuros:

- Parche exacto de cada paquete el día de `pnpm create` (dentro de las minors cerradas).
- El módulo Maven ya está en `backend/`. Crear `frontend/` (aún no existe).
- Cookie HttpOnly, refresh token, CORS explícito, `GET /me`, REST lista→carrito (requieren backend).
- Hosting, dominio y TLS de producción.
- Paginación del catálogo (backend).

---

## 21. Contradicciones / límites respecto al backend

No hay contradicción en contratos inventados: este blueprint se atiene a los controllers.

Límites que el frontend **no** debe tapar con APIs ficticias:

1. No hay `GET /api/v1/me`.
2. No hay REST de Sales, Invoice ni receipts; Ventas filtra `GET /api/v1/admin/orders`.
3. No hay REST para `AddShoppingListToCart`.
4. No hay CORS configurado.
5. No hay refresh token.
6. ADMIN no puede usar cart/checkout/assistant.
7. El simulador CARD no declina; el código `PAYMENT_DECLINED` igual existe.
8. Conversaciones Assistant no son durables.

---

## 22. Veredicto

Documento de arquitectura **objetivo**. Decisiones de stack, ubicación, sesión, API client y desarrollo local cerradas. Apto para inicializar `frontend/` sin modificar el backend cerrado.
