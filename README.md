# SuperFercho

Backend de supermercado virtual. Un único artefacto Maven (`com.superfercho:superfercho`) concentra identidad, catálogo, carrito, pedidos, pagos simulados, Knowledge y Assistant sobre los mismos casos de uso de Application.

## Estado del proyecto

El MVP backend está cerrado. Identity, Catalog, Shopping, Orders, Payments, Knowledge y Assistant están implementados e integrados. REST y Assistant son canales operativos.

## Arquitectura

Modular Monolith con Clean Architecture / Arquitectura Hexagonal.

Cada módulo separa Domain, Application, Infrastructure y Presentation. Domain no depende de Spring, JPA, HTTP, PostgreSQL, JWT ni LLM. Application declara casos de uso y ports. Infrastructure implementa adapters. Presentation expone REST.

Las dependencias apuntan hacia las reglas de negocio. Los módulos se comunican por ports y DTOs del consumidor, no por repositorios JPA ajenos.

[`docs/architecture/backend-technical-blueprint.md`](docs/architecture/backend-technical-blueprint.md) es documento de referencia arquitectónica. El código actual constituye la fuente de verdad de la implementación.

### Módulos

| Módulo | Responsabilidad |
|---|---|
| `identity` | Usuarios, autenticación JWT, direcciones y contexto del usuario actual |
| `catalog` | Categorías, productos, precio vigente, stock e inventario atómico |
| `shopping` | Carrito y shopping lists del cliente autenticado |
| `orders` | Checkout, pedidos, transiciones de estado, cancelación e idempotencia |
| `payments` | Ciclo de vida de pagos simulados e integración con checkout/cancelación |
| `knowledge` | Documentos, procesamiento, embeddings, pgvector y búsqueda semántica |
| `assistant` | Conversación, `LLMPort`, allowlist de tools y confirmación de acciones sensibles |
| `platform` | `Money` (COP), `Clock` y mapeo HTTP Problem Details |
| `mcp` | Paquete de integración; no es un módulo de negocio |

## Capacidades principales

### Identity

Registro de cliente, login, roles `CUSTOMER` / `ADMIN`, direcciones, dirección por defecto y ownership.

### Catalog

Categorías y productos (`ACTIVE` / `INACTIVE`), precio COP, stock, barcode opcional, imagen, búsqueda y listados. GET con `view` ausente o `PUBLIC` es el catálogo público. Mutaciones y `view=ADMIN` requieren `ADMIN`.

### Shopping

Un carrito activo por cliente, ítems, cantidades y listas de compras, con ownership del cliente autenticado.

### Orders

Checkout contra el carrito activo, pedidos y transiciones `PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DELIVERED`, o `PENDING` → `CANCELLED`.

### Payments

Métodos `SIMULATED_CARD` y `CASH_ON_DELIVERY`. REST: únicamente `GET /payments/{paymentId}` (`ADMIN`).

### Knowledge

Documentos, chunks, embeddings, pgvector y búsqueda semántica. REST restringido a `ADMIN`. El Assistant consulta Knowledge por use case, no por HTTP.

### Assistant

`POST /api/v1/assistant/chat` (`CUSTOMER`). Tools sobre Catalog, Shopping, Orders, Identity y Knowledge. Checkout y cancelación requieren confirmación explícita.

## Limitaciones del MVP

- Agregar una lista completa al carrito existe como caso de uso de Application y tool del Assistant; no hay endpoint REST.
- Conversaciones y tokens de confirmación del Assistant se mantienen en memoria de proceso.
- MCP no está operativo.
- Los pagos son simulados: no hay adquirente ni datos de tarjeta.
- El simulador de `SIMULATED_CARD` no genera `DECLINED`.
- No existe `GET /me` (ni equivalente) para el perfil del cliente.

## Reglas de negocio

- Las operaciones del cliente autenticado y la compra exigen JWT.
- Un producto `INACTIVE` no se agrega al carrito ni a listas y no se vende.
- `catalog` es propietario del stock. Shopping no muta inventario.
- El descuento de stock es atómico (`stock >= quantity` y `status = ACTIVE`).
- El checkout se ejecuta dentro de una única transacción local.
- `Idempotency-Key` identifica reintentos de checkout (retención 24 h).
- El precio cobrado es el vigente en catálogo; el cliente envía `expectedUnitPrice`.
- El pedido guarda snapshot de la dirección de envío.
- El cliente cancela solo en `PENDING` dentro de 15 minutos. Se restaura stock. Pago `APPROVED` se reembolsa (`refundedAt`); COD `PENDING` no.
- Pedidos `PENDING` elegibles se auto-confirman al superar esa ventana.
- El simulador aprueba `SIMULATED_CARD` y deja `CASH_ON_DELIVERY` en `PENDING`.
- Checkout y cancelación desde Assistant exigen confirmación explícita.

## Frontend

La UI vive en `frontend/` como aplicación Next.js independiente (App Router). Consume `/api/v1` del backend y no duplica Domain ni PostgreSQL.

La fundación técnica ya está creada. Login, catálogo, carrito, checkout, pedidos, listas, admin y assistant todavía no están implementados.

Detalle: [`frontend/README.md`](frontend/README.md). Blueprint: [`docs/architecture/frontend-technical-blueprint.md`](docs/architecture/frontend-technical-blueprint.md).

## Stack tecnológico

| Tecnología | Uso en SuperFercho |
|---|---|
| Java 21 | Compilación (`release 21`) |
| Spring Boot 3.x | Aplicación y wiring de adapters |
| Maven Wrapper | Build y tests |
| PostgreSQL 16 | Base de datos |
| pgvector | Embeddings y búsqueda vectorial de Knowledge |
| Spring Data JPA / Hibernate | Persistencia en Infrastructure (`ddl-auto=none`, `open-in-view=false`) |
| Flyway | Migraciones V1–V9 |
| JUnit 5 | Suite de tests |
| Mockito | Doubles en tests de Application |
| Testcontainers | PostgreSQL 16 + pgvector en integración |
| Docker | Motor para Testcontainers |
| Docker Compose | PostgreSQL local (`pgvector/pgvector:pg16`) |
| Next.js 16 / React 19 | Frontend en `frontend/` (fundación; sin features de negocio) |

## Persistencia

PostgreSQL es la única base. Flyway aplica las migraciones; Hibernate no altera el esquema (`spring.jpa.hibernate.ddl-auto=none`).

| Versión | Contenido |
|---|---|
| V1 | Extensión `vector` |
| V2 | Schema `identity` |
| V3 | Schema `catalog` |
| V4 | Schema `orders` |
| V5 | Schema `shopping` |
| V6 | Schema `payments` |
| V7 | Idempotencia de checkout |
| V8 | Schema `knowledge` |
| V9 | Embeddings (`vector(1536)`) |

Los IDs que cruzan módulos son UUID. No hay FK entre schemas de módulos distintos.

## API REST

Base: `/api/v1`. Errores en Problem Details (`application/problem+json`) con `code`. Rutas no listadas: `denyAll` (401 sin credenciales, 403 con JWT).

### Públicos

| Método | Path |
|---|---|
| `POST` | `/auth/login` |
| `POST` | `/customers` |
| `GET` | `/categories` |
| `GET` | `/categories/{categoryId}` |
| `GET` | `/products` |
| `GET` | `/products/search` |
| `GET` | `/products/{productId}` |

GET de catálogo: `view` ausente o `PUBLIC`. `view=ADMIN` no es público.

### CUSTOMER

| Método | Path | Notas |
|---|---|---|
| `POST` | `/addresses` | |
| `GET` | `/addresses` | |
| `PUT` | `/addresses/{addressId}` | |
| `DELETE` | `/addresses/{addressId}` | Desactivación lógica |
| `POST` | `/addresses/{addressId}/default` | |
| `GET` | `/cart` | |
| `DELETE` | `/cart` | Vacía el carrito |
| `POST` | `/cart/items` | |
| `PUT` | `/cart/items/{productId}` | |
| `DELETE` | `/cart/items/{productId}` | |
| `POST` | `/shopping-lists` | |
| `GET` | `/shopping-lists` | |
| `GET` | `/shopping-lists/{shoppingListId}` | |
| `PATCH` | `/shopping-lists/{shoppingListId}` | Rename |
| `POST` | `/shopping-lists/{shoppingListId}/items` | |
| `PATCH` | `/shopping-lists/{shoppingListId}/items/{productId}` | |
| `DELETE` | `/shopping-lists/{shoppingListId}/items/{productId}` | |
| `DELETE` | `/shopping-lists/{shoppingListId}/items` | Vacía ítems; no elimina la lista |
| `POST` | `/orders` | Header `Idempotency-Key` |
| `GET` | `/orders` | Query `page`, `size` |
| `GET` | `/orders/{orderId}` | Detalle con `payment` anidado |
| `POST` | `/orders/{orderId}/cancel` | |
| `POST` | `/assistant/chat` | |

### ADMIN

| Método | Path |
|---|---|
| `POST` | `/categories` |
| `PUT` | `/categories/{categoryId}` |
| `POST` | `/categories/{categoryId}/activate` |
| `POST` | `/categories/{categoryId}/deactivate` |
| `GET` | `/categories`, `/products`, `/products/search` con `view=ADMIN` |
| `POST` | `/products` |
| `PUT` | `/products/{productId}` |
| `POST` | `/products/{productId}/activate` |
| `POST` | `/products/{productId}/deactivate` |
| `POST` | `/products/{productId}/price` |
| `POST` | `/orders/{orderId}/status` |
| `GET` | `/admin/orders` |
| `GET` | `/admin/orders/{orderId}` |
| `GET` | `/payments/{paymentId}` |
| `POST` | `/knowledge/documents` |
| `GET` | `/knowledge/documents` |
| `GET` | `/knowledge/documents/{documentId}` |
| `PUT` | `/knowledge/documents/{documentId}/content` |
| `POST` | `/knowledge/documents/{documentId}/process` |
| `POST` | `/knowledge/documents/{documentId}/deactivate` |
| `POST` | `/knowledge/documents/{documentId}/reactivate` |
| `GET` | `/knowledge/search` |

`GET /admin/orders` acepta `page`, `size` y `status` opcional (repetido o separado por comas). Sin `status` devuelve todos los estados. Ventas usa `CONFIRMED,PREPARING,READY,DELIVERED`. El detalle de pedido (`GET /orders/{orderId}` y `GET /admin/orders/{orderId}`) incluye `payment` anidado cuando hay `paymentId`. El comprobante de compra es una vista de ese detalle; no hay `/receipt` ni `/admin/sales`. `GET /payments/{paymentId}` sigue siendo solo `ADMIN`.

## Seguridad

JWT Bearer, expiración 15 minutos. Roles `CUSTOMER` / `ADMIN` (`hasRole`). Ownership desde el sujeto del token (`CurrentUserProvider`); shopping, orders y assistant no aceptan `userId`/`customerId` en el body. Fallback: `anyRequest().denyAll()`. Secreto: `SUPERFERCHO_JWT_SECRET` (sin default). `.env` no se versiona.

## Testing

- Domain: invariantes de agregados.
- Application: casos de uso con ports/doubles.
- Integración: PostgreSQL 16 + pgvector (Testcontainers). Requiere Docker.
- REST y security: MockMvc + JWT.
- Casos cubiertos: checkout, concurrencia de stock, cancelación, pagos simulados, Knowledge, Assistant, productos `INACTIVE`.

Cierre de MVP: 954 tests ejecutados, 0 failures, 0 errors, 0 skipped.

## Requisitos

- JDK 21
- Docker (Testcontainers en las pruebas de integración)
- Docker Compose (PostgreSQL/pgvector local)
- Maven Wrapper (`backend/mvnw` / `backend/mvnw.cmd`)
- Node.js 20.9+ y pnpm 12.4.2 (solo para `frontend/`)

## Configuración

Plantilla local: `.env.example`. No commitear `.env`.

Compose (`.env.example`):

| Variable | Uso |
|---|---|
| `POSTGRES_DB` | Base del contenedor (default `superfercho`) |
| `POSTGRES_USER` | Usuario del contenedor |
| `POSTGRES_PASSWORD` | Contraseña del contenedor |
| `POSTGRES_PORT` | Puerto host (default `5432`) |

Aplicación (`.env.example` y `backend/src/main/resources/application.yml`):

| Variable | Uso |
|---|---|
| `SUPERFERCHO_DB_URL` | JDBC PostgreSQL |
| `SUPERFERCHO_DB_USERNAME` | Usuario JDBC |
| `SUPERFERCHO_DB_PASSWORD` | Contraseña JDBC |
| `SUPERFERCHO_JWT_SECRET` | Secreto JWT (obligatorio; sin default) |
| `OPENAI_API_KEY` | API key de embeddings y chat |
| `SUPERFERCHO_OPENAI_CHAT_URL` | URL de chat (default OpenAI) |
| `SUPERFERCHO_OPENAI_CHAT_MODEL` | Modelo de chat (default `gpt-4o-mini`) |
| `SUPERFERCHO_OPENAI_CHAT_CONNECT_TIMEOUT` | Timeout de conexión del chat |
| `SUPERFERCHO_OPENAI_CHAT_READ_TIMEOUT` | Timeout de lectura del chat |
| `SERVER_PORT` | Puerto HTTP (default `8080`) |
| `SUPERFERCHO_DEV_ADMIN_EMAIL` | Email del ADMIN local de desarrollo (solo perfil `local`; vacío = no se crea) |
| `SUPERFERCHO_DEV_ADMIN_PASSWORD` | Contraseña del ADMIN local de desarrollo (solo perfil `local`; vacío = no se crea) |

`backend/src/main/resources/application.yml` define además `SUPERFERCHO_OPENAI_EMBEDDINGS_URL` (default `https://api.openai.com/v1/embeddings`) y el modelo de embeddings `text-embedding-3-small`. Esa URL no está en `.env.example`.

## Desarrollo local

En Windows, desde la raíz del repositorio:

```powershell
.\scripts\dev.ps1
```

El script:

- levanta PostgreSQL con Docker Compose;
- espera a que el contenedor acepte conexiones;
- inicia Spring Boot en `:8080` y Next.js en `:3000` en ventanas aparte;
- usa un `.env` local (no versionado);
- genera un `SUPERFERCHO_JWT_SECRET` la primera vez y lo reutiliza después;
- arranca Spring Boot con el perfil `local`;
- si 5432, 8080 o 3000 ya están ocupados, muestra el proceso (nombre y PID) y se detiene; no mata procesos existentes.

### ADMIN local de desarrollo

El perfil `local` puede crear o actualizar de forma idempotente un usuario `ADMIN` de desarrollo. No corre fuera de ese perfil y no expone ningún endpoint.

En `.env` (nunca en Git) configura:

```
SUPERFERCHO_DEV_ADMIN_EMAIL=
SUPERFERCHO_DEV_ADMIN_PASSWORD=
```

Si alguna de las dos está vacía, el arranque no inventa credenciales y omite el bootstrap. Si ambas están definidas, se crea el ADMIN o se actualiza el usuario con ese email para que quede `ADMIN` / `ACTIVE` con esa contraseña (BCrypt). Volver a arrancar no crea un duplicado.

Esas credenciales son solo locales. No las versionar. El registro de clientes sigue creando únicamente `CUSTOMER`.

Requisitos: Docker Desktop, JDK 21, Node.js 20.9+ (el frontend se arranca con `corepack.cmd pnpm`). Embeddings y chat del Assistant siguen necesitando `OPENAI_API_KEY` en `.env` si se usan.

## Ejecución

### PostgreSQL

```bash
docker compose up -d
```

Compose publica `pgvector/pgvector:pg16` en el puerto 5432. No arranca Spring Boot.

### Aplicación

Windows:

```bat
cd backend
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Unix:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Puerto: `SERVER_PORT` (8080). Requiere `SUPERFERCHO_JWT_SECRET`. Embeddings y chat requieren `OPENAI_API_KEY`. El bootstrap del ADMIN local solo corre con el perfil `local` y las variables `SUPERFERCHO_DEV_ADMIN_EMAIL` / `SUPERFERCHO_DEV_ADMIN_PASSWORD` definidas en el entorno.

### Frontend

```bash
cd frontend
corepack pnpm install
corepack pnpm dev
```

Origen: `http://localhost:3000`. Rewrite de `/api/v1` hacia Spring en `8080`. Detalle en [`frontend/README.md`](frontend/README.md).

### Pruebas

Windows:

```bat
cd backend
mvnw.cmd test
```

Unix:

```bash
cd backend
./mvnw test
```

Las IT de persistencia usan Testcontainers y necesitan el daemon Docker. Docker Compose no forma parte de `mvnw test`.
