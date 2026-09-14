# SuperFercho Backend Technical Architecture Blueprint

**Status:** Authoritative architectural specification for implementation  
**Type:** Documentation only (no application source)  
**Baseline:** Modular monolith, Clean / Hexagonal Architecture  
**Companion rules (unchanged by this document):** `.cursor/rules/00-global-architecture.mdc`, `01-java-spring.mdc`, `02-testing.mdc`, `03-git-workflow.mdc`, `04-ai-rag-mcp.mdc`

This blueprint integrates the approved MVP decisions. It replaces earlier candidate designs where those candidates conflicted with the decisions below.

Cursor architecture rules under `.cursor/rules/` are **not** modified by this document. One remaining rules-file mismatch is recorded in §19 and in the consistency audit.

---

## 0. Approved technology and module baseline

| Concern | Decision |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x (exact minor deferred) |
| Build | Maven, **single module**, Maven Wrapper |
| Architecture | Modular monolith, Clean / Hexagonal |
| Database | One PostgreSQL database |
| Knowledge vectors | pgvector in that same PostgreSQL database |
| Persistence | Spring Data JPA / Hibernate |
| Schema evolution | Flyway (immutable applied migrations; never `ddl-auto=update`) |
| API | REST under `/api/v1` |
| Security | Spring Security, JWT access tokens |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, Testcontainers PostgreSQL |
| Integration-test DB | PostgreSQL (+ pgvector when Knowledge is tested). **H2 is not used** |

**Forbidden unless a later explicit authorization changes architecture:**

microservices, Kafka, RabbitMQ for internal architecture, Redis, Elasticsearch, Kubernetes, Saga, XA, distributed transactions, a second database, a second vector database, LangChain, LangGraph, or another AI orchestration framework.

**Business modules:** `identity`, `catalog`, `shopping`, `orders`, `payments`, `knowledge`, `assistant`.

**MCP** is an integration / tool-exposure layer, **not** a business module.

---

## 1. Project structure

### 1.1 Maven layout (single module)

```text
superfercho/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/
├── src/main/java/com/superfercho/
│   ├── SuperFerchoApplication.java
│   ├── identity/
│   ├── catalog/
│   ├── shopping/
│   ├── orders/
│   ├── payments/
│   ├── knowledge/
│   ├── assistant/
│   ├── mcp/
│   └── platform/
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── db/migration/
└── src/test/
    ├── java/com/superfercho/
    └── resources/application-test.yml
```

Do not create a Maven multi-module build for the MVP.

### 1.2 Package tree per business module

Empty packages are not created. `domain/service` is omitted until a real domain service exists.

```text
com.superfercho.identity
├── domain/model | repository | exception
├── application/usecase | dto | port
├── infrastructure/persistence | security | configuration
└── presentation/rest

com.superfercho.catalog
├── domain/model | repository | exception
├── application/usecase | dto | port          # includes ProductImageStoragePort
├── infrastructure/persistence | storage | configuration
└── presentation/rest

com.superfercho.shopping
├── domain/model | repository | exception
├── application/usecase | dto | port          # ProductCatalogPort
├── infrastructure/persistence | catalog | configuration
└── presentation/rest

com.superfercho.orders
├── domain/model | repository | exception
├── application/usecase | dto | port          # ShoppingCartPort, CustomerAddressPort,
│                                             # InventoryPort, PaymentPort
├── infrastructure/persistence | shopping | identity | catalog
│                             | payments | scheduling | configuration
└── presentation/rest

com.superfercho.payments
├── domain/model | repository | exception
├── application/usecase | dto
├── infrastructure/persistence | configuration
└── (no public payment resource)

com.superfercho.knowledge
├── domain/model | repository | exception
├── application/usecase | dto | port          # KnowledgeVectorStorePort, EmbeddingPort
├── infrastructure/persistence | vector | embedding | configuration
└── presentation/rest                         # HTTP contract deferred to Knowledge phase

com.superfercho.assistant
├── domain/model | repository | exception
├── application/usecase | dto | port | tool   # LLMPort + consumer ports to use cases
├── infrastructure/persistence | llm | catalog | shopping | orders | knowledge | configuration
└── presentation/rest

com.superfercho.mcp
├── tool
└── configuration
```

### 1.3 Platform package (minimal)

```text
com.superfercho.platform
├── money                       # Money value object (COP)
├── time                        # Clock bean (UTC)
└── error                       # RFC 7807 problem mapping (presentation)
```

`Money` lives here because Catalog, Shopping, Orders, and Payments share the same monetary concept. The platform layer must not accumulate unrelated utilities.

MCP stays beside modules. Assistant owns conversations and orchestration; MCP is a driving adapter like REST.

---

## 2. Module dependencies

Logical direction inside one deployable JAR:

```text
REST / MCP / Assistant tools
        ↓
application use cases
        ↓
domain

infrastructure adapters implement ports
and may call another module's application contract only
```

| Consumer | Provider | Port / mechanism | Forbidden |
|---|---|---|---|
| Shopping → Catalog | Product query / availability | `ProductCatalogPort` | Catalog JPA/repositories |
| Orders → Shopping | Read / clear cart | `ShoppingCartPort` | Shopping persistence |
| Orders → Identity | Load usable address | `CustomerAddressPort` | Identity persistence |
| Orders → Catalog | Atomic stock + sale snapshot | `InventoryPort` | Catalog persistence |
| Orders → Payments | Charge / refund simulated payment | `PaymentPort` | Payments persistence |
| Assistant → business modules | Same use cases as REST | Assistant-owned ports → application use cases | Any other module’s DB/JPA |
| Knowledge → pgvector | Vector insert/search | `KnowledgeVectorStorePort` | Second vector database |
| MCP → business modules | None as owner | MCP tool → application use case | MCP → repository/DB |

Provider modules **must not** depend on their consumers. Catalog, Identity, Payments, and Knowledge must not depend on Orders, Shopping, or Assistant.

No module may depend on another module’s domain entities, JPA entities, repositories, or infrastructure classes.

### Consumer-owned ports

The consuming module defines the port. An adapter in the consumer’s infrastructure layer calls the provider’s **application** contract.

Example — checkout:

```text
orders.application.usecase.Checkout
  → PaymentPort, InventoryPort, ShoppingCartPort, CustomerAddressPort
      ↑ orders.infrastructure.* adapters
      ↓ provider application use cases
```

`CurrentUserProvider` is an Identity security/application identity port used by driving adapters (REST, Assistant, MCP). Use cases receive an explicit `Actor` / `UserId`. Domain objects never read Spring `SecurityContext`.

---

## 3. Domain model

No domain services are required for the MVP. Invariants belong on aggregates. Checkout is an application use case.

### 3.1 Identity — one `User` aggregate

There is **no** separate Customer or Admin aggregate. A user has **exactly one** role: `CUSTOMER` or `ADMIN`.

Public registration creates `CUSTOMER` only. Users cannot self-register as `ADMIN`. The first `ADMIN` is provisioned through controlled bootstrap/administrative configuration, not the public register endpoint.

For the MVP, `ADMIN` is **not** required to behave as a `CUSTOMER`.

| Kind | Name |
|---|---|
| Aggregate | `User` |
| Entity | `Address` (inside the User aggregate) |
| VO | `UserId`, `Email`, `HashedPassword`, `DocumentIdentity`, `Role`, `UserStatus`, `AddressId`, `Phone` |
| Repository | `UserRepository` |
| Exceptions | `DuplicateEmail`, `DuplicateDocumentIdentity`, `InvalidAddress`, `DefaultAddressInvariantViolation`, `UserNotFound`, `AddressNotFound`, `AddressNotUsable` |

**User fields:** `id` (UUID), `documentType`, `documentNumber`, `fullName`, `email`, `phone`, `passwordHash`, `role`, `status`, timestamps.

`documentType` is a simple business field (opaque string or a small closed set). Do not add Colombian tax, RUT/NIT, or check-digit rules.

**Address fields:** `label`, `recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`, `isDefault`, `status`, timestamps.

Rules:

- `email` unique; `documentType` + `documentNumber` unique
- multiple addresses per user
- at most one **active** default address
- addresses are **deactivated**, not physically deleted, when history must be preserved
- inactive addresses cannot be selected for new checkout
- orders store an immutable shipping-address snapshot (not a live address row)

### 3.2 Catalog

| Kind | Name |
|---|---|
| Aggregate | `Product` |
| Aggregate | `Category` |
| VO | `ProductId`, `CategoryId`, `Money`, `Barcode`, `StockQuantity`, `CatalogStatus` |
| Repository | `ProductRepository`, `CategoryRepository` |
| Exceptions | `InvalidPrice`, `InvalidStock`, `ProductNotFound`, `ProductInactive`, `CategoryInactive`, `DuplicateBarcode`, `InsufficientStock` |

**Category 1 → N Products.** A product has exactly one main category. No many-to-many.

**Product fields:** `id`, category reference, optional unique barcode, `name`, `brand`, `description`, `price` (`Money`, COP), `stock`, image reference/URL, `status`, timestamps.

Rules:

- `price >= 0`, `stock >= 0`
- `stock = 0` means unavailable / out of stock
- inactive products cannot be publicly purchased or publicly fetched (public GET → 404)
- inactive categories cannot be publicly used
- Catalog owns stock
- historical products/categories are **not physically deleted** when orders or other history still need the row; HTTP `DELETE` on admin catalog resources means **deactivation**

### 3.3 Shopping

| Kind | Name |
|---|---|
| Aggregate | `Cart` |
| Entity | `CartItem` |
| Aggregate | `ShoppingList` |
| Entity | `ShoppingListItem` |
| VO | `CartId`, `ShoppingListId`, `Quantity` |
| Repository | `CartRepository`, `ShoppingListRepository` |
| Exceptions | `CartNotFound`, `EmptyCart`, `InvalidQuantity`, `ShoppingListNotFound`, `ProductUnavailableForCart` |

- one active cart per user
- one product line per product in a cart
- quantity > 0
- stock is **not** reserved when adding to cart or lists
- cart stored price is **informational only**; Catalog current price is authoritative at checkout
- adding list items into a cart (application use case / assistant tool, or client repeating `POST /cart/items`) must use the same cart validation as adding a single cart item

The MVP REST contract does not include a dedicated “add list to cart” path. That behavior remains an application capability; the HTTP client may compose `POST /api/v1/cart/items`. Do not add extra REST paths beyond §10.

### 3.4 Orders

| Kind | Name |
|---|---|
| Aggregate | `Order` |
| Entity | `OrderItem` |
| VO | `OrderId`, `OrderStatus`, `ShippingAddressSnapshot`, `OrderItemSnapshot`, `CancellationWindow` |
| Repository | `OrderRepository` |
| Exceptions | `InvalidOrderTransition`, `OrderNotCancellable`, `CancellationWindowExpired`, `OrderNotFound`, `OrderNotOwned`, `EmptyCheckout`, `PriceChanged`, `CheckoutConflict` |

**Authoritative order states:** `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`.

**Valid transitions only:**

```text
PENDING    → CONFIRMED
PENDING    → CANCELLED
CONFIRMED  → PREPARING
PREPARING  → READY
READY      → DELIVERED
```

No arbitrary jumps (for example `PENDING → DELIVERED` or `CANCELLED → CONFIRMED`).

Customer cancellation: only while `PENDING` and within 15 minutes of order creation / payment approval (COD timer starts at order creation; simulated card at payment approval, which is the same commit instant in this MVP). Cancellation restores stock. If payment was `APPROVED` simulated card, payment becomes `REFUNDED`. COD payment stays `PENDING` in the MVP.

After 15 minutes, remaining `PENDING` orders are auto-confirmed (`PENDING → CONFIRMED`). Customers then cannot cancel.

Idempotency storage is application/infrastructure owned by Orders, not a domain aggregate.

### 3.5 Payments

| Kind | Name |
|---|---|
| Aggregate | `Payment` |
| VO | `PaymentId`, `PaymentMethod`, `PaymentStatus`, `Money` |
| Repository | `PaymentRepository` |
| Exceptions | `UnsupportedPaymentMethod`, `InvalidPaymentState`, `PaymentNotFound` |

**Methods:** `SIMULATED_CARD`, `CASH_ON_DELIVERY`.

**MVP statuses:** `PENDING`, `APPROVED`, `DECLINED`, `REFUNDED`.

`PAID` is **not** an MVP status. A later delivery/cash-collection phase may introduce `PAID` for COD. That is deferred.

Never store card number, CVV, expiration date, or full payment credentials.

**Status mapping:**

| Method | Success at checkout | Decline | Customer cancel in window |
|---|---|---|---|
| `SIMULATED_CARD` | `APPROVED`; order `PENDING` | checkout fails; **order is not created** | payment `REFUNDED`; stock restored |
| `CASH_ON_DELIVERY` | payment `PENDING`; order `PENDING` | not applicable at checkout | payment remains `PENDING`; stock restored |

There is no real external refund provider. `REFUNDED` is a local simulated-card state change inside the cancel transaction.

`DECLINED` means the simulated charge failed. Checkout rolls back; no successful order. Failed checkout must not leave partial business state (no order, stock unchanged, cart unchanged). Persisting a `DECLINED` row is not required for the MVP; the HTTP 409/business problem is sufficient.

### 3.6 Knowledge

| Kind | Name |
|---|---|
| Aggregate | `KnowledgeDocument` |
| Entity | `KnowledgeChunk` |
| VO | `DocumentId`, `ChunkId`, `DocumentMetadata` |
| Repository | `KnowledgeDocumentRepository` |
| Exceptions | `DocumentNotFound`, `InvalidDocument` |

Embeddings are infrastructure behind `KnowledgeVectorStorePort`. Chunking/embedding provider/dimension are deferred to the Knowledge phase.

RAG is informational (recipes, guides, education). It is **not** the source of truth for stock, price, cart, orders, payments, or identity.

### 3.7 Assistant

| Kind | Name |
|---|---|
| Aggregate | `Conversation` |
| Entity | `Message` |
| VO | `ConversationId`, `MessageRole`, `ToolCallId` |
| Repository | `ConversationRepository` (Assistant’s tables only) |
| Exceptions | `ConversationNotFound`, `ConversationNotOwned`, `ToolNotAllowed`, `ConfirmationRequired`, `InvalidToolArguments` |

Assistant domain does not own Product, Cart, Order, or Payment types. The public REST surface is only `POST /api/v1/assistant/chat`; conversations may still be persisted internally.

---

## 4. Application use cases (MVP)

### Identity

- `RegisterCustomer` — public; role `CUSTOMER` only
- `AuthenticateUser`
- `GetCurrentUser`
- `ListAddresses`, `AddAddress`, `UpdateAddress`, `DeactivateAddress`
- `GetAddressForCustomer` — application contract for Orders (not a public “pass any userId” API)
- `ProvisionBootstrapAdmin` — controlled configuration, not public registration

### Catalog

- `CreateCategory`, `UpdateCategory`, `DeactivateCategory`, `ListPublicCategories`
- `CreateProduct`, `UpdateProduct`, `DeactivateProduct`
- `GetPublicProduct`, `SearchPublicProducts`
- `GetProductForSale` — internal sale snapshot for Shopping/Orders adapters
- `DecrementStock`, `RestoreStock`
- `StoreProductImage` — used when an admin product write includes an image; local filesystem adapter

### Shopping

- `GetOrCreateActiveCart`, `GetCart`
- `AddCartItem`, `UpdateCartItemQuantity`, `RemoveCartItem`, `ClearCart`
- `CreateShoppingList`, `UpdateShoppingList`, `ListShoppingLists`, `GetShoppingList`, `DeactivateOrDeleteShoppingList`
- `AddShoppingListToCart` — application/tool capability; same validation as `AddCartItem` (no extra REST path in the MVP contract)

### Orders

- `Checkout` — transaction boundary for purchase
- `GetCustomerOrders`, `GetOrderForCustomer`
- `CancelOrder`
- `ListOrdersForAdmin`, `GetOrderForAdmin`, `UpdateOrderStatus`
- `AutoConfirmPendingOrders`

### Payments

- `ProcessSimulatedPayment` — local, joins caller transaction; not a public API
- `RefundSimulatedCardPayment` — local `APPROVED` → `REFUNDED`; invoked from cancel via `PaymentPort`
- `GetPaymentForOrder` — internal; order responses may embed a summary

### Knowledge

- `CreateKnowledgeDocument`, `ChunkKnowledgeDocument`, `GenerateEmbeddings`, `SearchKnowledge`

HTTP for Knowledge is **not** in the MVP REST contract. Presentation is deferred to the Knowledge phase. Assistant retrieval uses `SearchKnowledge` as an application use case.

### Assistant

- `Chat` — authenticated; identity from `CurrentUserProvider`; may start or continue an internal conversation
- `ExecuteApprovedTool` — allowlisted tools, confirmation for checkout

### MCP

No business use cases. Tools delegate to the use cases above.

---

## 5. Cross-module ports

### `ProductCatalogPort`

- **Owner:** Shopping
- **Purpose:** Current name, status, category usability, COP price, stock (informational; no reservation)
- **Input:** `ProductId` (+ quantity when checking availability)
- **Output:** sale snapshot
- **Adapter:** `shopping.infrastructure.catalog` → Catalog `GetProductForSale`

### `ShoppingCartPort`

- **Owner:** Orders
- **Purpose:** Load the authenticated user’s active cart; clear after successful checkout
- **Input:** `UserId` from auth
- **Output:** lines `(productId, quantity)`; optional display price must not be charged
- **Adapter:** `orders.infrastructure.shopping` → Shopping `GetCart` / `ClearCart`

### `CustomerAddressPort`

- **Owner:** Orders
- **Purpose:** Load an **active** address owned by the authenticated user
- **Input:** `UserId` + `AddressId`
- **Output:** address fields for snapshot, or not found / not owned / inactive
- **Adapter:** `orders.infrastructure.identity` → Identity `GetAddressForCustomer`

### `InventoryPort`

- **Owner:** Orders
- **Purpose:** Re-validate saleability; atomic decrement; restore on cancel
- **Input:** `(ProductId, quantity)` lines
- **Output:** snapshots `(id, name, unit Money)` or insufficient-stock product ids
- **Adapter:** `orders.infrastructure.catalog` → Catalog `DecrementStock` / `RestoreStock`

Not a duplicate of `ProductCatalogPort`. Shopping must not decrement stock.

### `PaymentPort`

- **Owner:** Orders
- **Purpose:** Simulated card or COD; refund approved simulated card on cancel
- **Input:** checkout correlation, `Money` (COP), method, non-sensitive simulation outcome; refund: payment/order id
- **Output:** payment id + `PaymentStatus`
- **Adapter:** `orders.infrastructure.payments` → Payments application use cases

Deterministic/testable simulator. No PAN/CVV.

### `CurrentUserProvider`

- **Owner:** Identity (security)
- **Purpose:** Authenticated principal for REST, Assistant, MCP
- **Output:** `UserId`, `Role`, `status`
- **Adapter:** `identity.infrastructure.security`
- Domain must not call `SecurityContext`

### `KnowledgeVectorStorePort`

- **Owner:** Knowledge
- **Purpose:** Vector insert/search; MVP adapter is PostgreSQL + pgvector
- **Adapter:** `knowledge.infrastructure.vector`

### `EmbeddingPort`

- **Owner:** Knowledge
- **Purpose:** Isolate embedding vendor (vendor/model/dimension deferred)
- **Adapter:** `knowledge.infrastructure.embedding`
- Tests use a deterministic fake

### `LLMPort`

- **Owner:** Assistant
- **Purpose:** Isolate chat + tool-calling (provider deferred)
- **Adapter:** `assistant.infrastructure.llm`
- Tests use a deterministic fake

### `ProductImageStoragePort`

- **Owner:** Catalog (infrastructure port)
- **Purpose:** Store/replace image bytes; return a URL/reference stored on the product
- **MVP adapter:** local filesystem
- **Not in MVP:** S3, Cloudinary, Azure Blob, or any cloud object store
- The provider remains replaceable behind the port
- Database stores only the resulting URL/reference
- Admin `POST`/`PUT` product resources may include an image part; there is no separate public image endpoint

---

## 6. Database design

One PostgreSQL database. Flyway is authoritative. `spring.jpa.hibernate.ddl-auto` is `validate` or `none`, never `update`.

**Schema per business module:** `identity`, `catalog`, `shopping`, `orders`, `payments`, `knowledge`, `assistant`.

Still one deployable, one DataSource, one local transaction manager.

**Cross-module references:** logical UUIDs, **no** physical foreign keys.  
**Intra-module relationships:** real PostgreSQL foreign keys, unique constraints, and CHECKs.

Technical keys: UUID. Monetary amounts: `NUMERIC(12,2)` with currency stored as `COP` (or omitted as a column if the application always implies COP; prefer an explicit `currency CHAR(3)` CHECK (`currency = 'COP'`) so persistence cannot silently become multi-currency).

Canonical time: **UTC** `TIMESTAMPTZ`.

### 6.1 Identity

**`identity.users`**

- `id` UUID PK
- `email` VARCHAR NOT NULL UNIQUE
- `password_hash` VARCHAR NOT NULL
- `document_type` VARCHAR NOT NULL
- `document_number` VARCHAR NOT NULL
- UNIQUE (`document_type`, `document_number`)
- `full_name` VARCHAR NOT NULL
- `phone` VARCHAR NOT NULL
- `role` VARCHAR NOT NULL CHECK (`role` IN ('CUSTOMER', 'ADMIN'))
- `status` VARCHAR NOT NULL CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
- `created_at` / `updated_at` TIMESTAMPTZ NOT NULL

**`identity.addresses`**

- `id` UUID PK
- `user_id` UUID NOT NULL FK → `users.id`
- `label` VARCHAR NOT NULL
- `recipient_name` VARCHAR NOT NULL
- `address_line` VARCHAR NOT NULL
- `additional_info` VARCHAR NULL
- `city` VARCHAR NOT NULL
- `department` VARCHAR NOT NULL
- `phone` VARCHAR NOT NULL
- `is_default` BOOLEAN NOT NULL
- `status` VARCHAR NOT NULL CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
- timestamps
- Partial unique: one active default per user  
  `CREATE UNIQUE INDEX … ON identity.addresses (user_id) WHERE is_default AND status = 'ACTIVE'`
- Index `(user_id, status)`

### 6.2 Catalog

**`catalog.categories`**

- `id` UUID PK
- `name` VARCHAR NOT NULL
- `status` VARCHAR NOT NULL CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
- timestamps

**`catalog.products`**

- `id` UUID PK
- `category_id` UUID NOT NULL FK → `categories.id`
- `name` VARCHAR NOT NULL
- `brand` VARCHAR NOT NULL
- `description` TEXT NULL
- `barcode` VARCHAR NULL; unique where NOT NULL
- `price_amount` NUMERIC(12,2) NOT NULL CHECK (`price_amount` >= 0)
- `currency` CHAR(3) NOT NULL CHECK (`currency` = 'COP')
- `stock` INTEGER NOT NULL CHECK (`stock` >= 0)
- `image_uri` VARCHAR NULL
- `status` VARCHAR NOT NULL CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
- `version` BIGINT NOT NULL DEFAULT 0
- timestamps
- Indexes: `(category_id, status)`, `(status)`, name/brand text as needed for ILIKE

Atomic decrement:

```text
UPDATE catalog.products
   SET stock = stock - :qty, version = version + 1
 WHERE id = :id AND status = 'ACTIVE' AND stock >= :qty
```

Zero rows → insufficient stock. This is the concurrency control.

### 6.3 Shopping

**`shopping.carts`**

- `id` UUID PK
- `user_id` UUID NOT NULL (logical)
- `status` VARCHAR NOT NULL (active cart)
- Partial unique: one active cart per user
- timestamps

**`shopping.cart_items`**

- `id` UUID PK
- `cart_id` UUID NOT NULL FK → `carts.id`
- `product_id` UUID NOT NULL (logical)
- `quantity` INTEGER NOT NULL CHECK (`quantity` > 0)
- UNIQUE (`cart_id`, `product_id`)
- optional `last_seen_unit_price` NUMERIC NULL (display only)

**`shopping.shopping_lists`** / **`shopping.shopping_list_items`**

- list: `id`, `user_id` (logical), `name`, timestamps
- items: FK to list, `product_id` (logical), `quantity` > 0, UNIQUE (`list_id`, `product_id`)

### 6.4 Orders

**`orders.orders`**

- `id` UUID PK
- `user_id` UUID NOT NULL (logical)
- `status` VARCHAR NOT NULL CHECK IN (`PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`)
- `payment_id` UUID NULL (logical)
- `payment_method` VARCHAR NOT NULL
- `total_amount` NUMERIC(12,2) NOT NULL CHECK (`total_amount` >= 0)
- `currency` CHAR(3) NOT NULL CHECK (`currency` = 'COP')
- shipping snapshot columns: `ship_label`, `ship_recipient_name`, `ship_address_line`, `ship_additional_info`, `ship_city`, `ship_department`, `ship_phone`
- `source_address_id` UUID NULL (logical)
- `placed_at` TIMESTAMPTZ NOT NULL
- `cancellation_deadline_at` TIMESTAMPTZ NOT NULL
- `confirmed_at` / `cancelled_at` NULL
- `version` BIGINT NOT NULL
- Indexes: `(user_id, placed_at DESC)`, `(status, placed_at)` / `(status, cancellation_deadline_at)`

**`orders.order_items`**

- FK to order
- `product_id` UUID NOT NULL (logical)
- `product_name` VARCHAR NOT NULL (snapshot)
- `unit_price` NUMERIC(12,2) NOT NULL
- `quantity` INTEGER NOT NULL CHECK (`quantity` > 0)
- `line_total` NUMERIC(12,2) NOT NULL

**`orders.checkout_idempotency`**

- `user_id` UUID NOT NULL
- `idempotency_key` VARCHAR NOT NULL
- UNIQUE (`user_id`, `idempotency_key`)
- `request_hash` VARCHAR NOT NULL
- `order_id` UUID NULL (logical)
- `response_payload` JSONB NULL
- `created_at` TIMESTAMPTZ NOT NULL
- successful-record retention: **24 hours** (application/purge policy; not a second store)

Do not introduce `FAILED` / `RECOVERING` states unless a later implementation proves they are necessary. Concurrent duplicates are handled by the unique constraint and row lock inside the checkout transaction.

### 6.5 Payments

**`payments.payments`**

- `id` UUID PK
- `order_id` UUID NOT NULL (logical)
- `method` VARCHAR NOT NULL CHECK IN (`SIMULATED_CARD`, `CASH_ON_DELIVERY`)
- `status` VARCHAR NOT NULL CHECK IN (`PENDING`, `APPROVED`, `DECLINED`, `REFUNDED`)
- `amount` NUMERIC(12,2) NOT NULL CHECK (`amount` >= 0)
- `currency` CHAR(3) NOT NULL CHECK (`currency` = 'COP')
- `provider_reference` VARCHAR NULL (simulator id only)
- timestamps

No PAN, CVV, expiry columns.

### 6.6 Knowledge

Flyway: `CREATE EXTENSION IF NOT EXISTS vector;`

**`knowledge.documents`**, **`knowledge.chunks`** with `embedding VECTOR(n)` where **n is deferred** to the Knowledge phase, plus an HNSW/IVFFlat index choice also deferred.

### 6.7 Assistant

**`assistant.conversations`**, **`assistant.messages`** with `user_id` logical UUID. No FKs to other modules.

---

## 7. Transaction boundaries

`@Transactional` belongs on application use cases only. Single local PostgreSQL transaction. No Saga, XA, brokers, or outbox.

| Use case | Boundary |
|---|---|
| Register / address mutations | One TX; unique and partial-unique constraints enforce invariants |
| Cart / shopping-list writes | One TX on shopping tables only; catalog is read via port; **no stock write** |
| **Checkout** | **One TX for all participating PostgreSQL writes** (idempotency, stock, payment, order, cart clear) |
| `ProcessSimulatedPayment` | Joins the caller TX |
| `CancelOrder` | One TX: order `CANCELLED`, stock restore, simulated-card `REFUNDED` when applicable |
| `AutoConfirmPendingOrders` | **One TX per order**; scheduler must not wrap the batch |

Failed checkout rolls back completely.

---

## 8. Checkout sequence

**Endpoint:** `POST /api/v1/checkout`  
**Auth:** JWT, role `CUSTOMER`  
**Header:** `Idempotency-Key` required  
**Currency:** COP  
**Transaction:** one local PostgreSQL transaction on `Checkout`

Payment simulation participates in that same caller transaction.

### 8.1 Outside the transaction

1. Authenticate. `CurrentUserProvider` yields `UserId`. Reject any client-supplied user/customer id.
2. Require `Idempotency-Key`. Missing/blank → 400.

### 8.2 Inside `Checkout`

3. **Idempotency.** Lock/insert `(user_id, idempotency_key)` in the same TX.
   - Same key + same logical request + existing success → return the original successful order (24h retention).
   - Same key + different request hash → **409 Conflict**.
4. **Address.** Active, owned by the user. Inactive → not selectable.
5. **Cart.** Active cart; empty → 409.
6. **Product availability/status.** Each line must be active, category usable, stock sufficient to sell.
7. **Explicit price acceptance.** Request includes accepted unit prices (COP) per product. Catalog current price is the source of truth. Cart prices are ignored for charging.
8. If any current price ≠ accepted price → **409 Conflict** listing affected `productId`, current price, accepted price. Do **not** silently charge the new price. Client retries with current prices.
9. **Atomic stock decrement** via `InventoryPort` (`UPDATE … WHERE stock >= qty`). Concurrent checkouts cannot produce negative stock.
10. **Payment** via `PaymentPort`.
    - `SIMULATED_CARD` `DECLINED` → throw; full rollback; no order.
    - `SIMULATED_CARD` `APPROVED` → continue.
    - `CASH_ON_DELIVERY` → payment `PENDING`.
11. **Create order** `PENDING` with item snapshots (name, unit price, qty) and shipping-address snapshot. `placed_at = clock.instant()` (UTC). `cancellation_deadline_at = placed_at + 15 minutes`.
12. **Clear cart.**
13. Persist idempotency success payload.
14. Commit.

### 8.3 Failure matrix

| Failure | Persisted business state | HTTP |
|---|---|---|
| Unauthenticated | none | 401 |
| Not CUSTOMER | none | 403 |
| Missing Idempotency-Key | none | 400 |
| Validation | none | 400 |
| Same key, same request, prior success | original order | 200 |
| Same key, different request | none new | 409 |
| Address missing / not owned / inactive | none | 404 or 409 as appropriate (inactive/unusable → 409; unknown → 404) |
| Empty cart | none | 409 |
| Inactive/unavailable product | none | 409 |
| Price changed | none | 409 with product/price details |
| Insufficient stock / concurrent oversell loser | none | 409 |
| Payment declined | none | 409 |
| Unexpected error | rollback | 500 problem+json, no stack trace |

No distributed compensation. Rollback is the compensation.

---

## 9. Security architecture

- Spring Security, stateless JWT Bearer access token.
- **Access token lifetime ≈ 15 minutes.**
- **No refresh-token subsystem**, rotation, or revocation infrastructure in the MVP.
- Password hashing: BCrypt (or the Spring `PasswordEncoder` default equivalent). Never log passwords, tokens, or JWT secrets.
- JWT secret from environment/configuration, never hard-coded.
- `CurrentUserProvider` is the identity abstraction for presentation/application adapters.
- Domain never accesses `SecurityContext`.
- Personalized operations use the authenticated principal. Clients **must not** send arbitrary `userId` / `customerId` for ownership-sensitive operations. Ownership checks are mandatory in application logic.
- `CUSTOMER`: `/users/me`, addresses, cart, shopping lists, checkout, own orders, cancel own pending order, assistant chat.
- `ADMIN`: admin catalog, admin orders. ADMIN is not required to shop.
- Assistant and MCP inherit the same authenticated identity. The model cannot supply a user id to impersonate another user.
- No public payment, inventory, stock, or order-payment endpoints.

---

## 10. REST API contract (authoritative)

**Base path:** `/api/v1`

This is the **MVP REST contract**, not a candidate list. Do not add endpoints without a new architectural approval.

### 10.1 Conventions

| Topic | Rule |
|---|---|
| Pagination | `page` default **0**, `size` default **20**, maximum **100** |
| Public inactive product | **404 Not Found** (do not leak inactive catalog as 200) |
| Identity | From authenticated principal only |
| Ownership | Mandatory in application logic |
| Admin routes | `ADMIN` role |
| Error media type | `application/problem+json` (RFC 7807) |
| Error body | RFC 7807 fields; may include a stable application-specific `code` |
| 400 | Validation / bad request |
| 401 | Unauthenticated |
| 403 | Authenticated but not allowed |
| 404 | Resource not available (including public inactive product) |
| 409 | Business or concurrency conflict (price change, stock, idempotency mismatch, illegal transition, payment declined, default-address invariant, duplicates) |
| 500 | Unexpected server error (no stack traces, no secrets) |

Request/response bodies are application DTOs at the presentation boundary. Controllers stay thin and call use cases.

### 10.2 Auth (public)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | no | Register `CUSTOMER`. Body: email, password, fullName, documentType, documentNumber, phone. 409 on duplicate email/document. |
| POST | `/api/v1/auth/login` | no | Authenticate. Body: email, password. Response: access token (~15 min) + user summary. 401 invalid credentials. |

### 10.3 Current user and addresses (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/users/me` | Current user profile (no password hash). |
| GET | `/api/v1/users/me/addresses` | List addresses. |
| POST | `/api/v1/users/me/addresses` | Create address (`isDefault` allowed; enforces one active default). |
| PUT | `/api/v1/users/me/addresses/{id}` | Replace/update owned address. |
| DELETE | `/api/v1/users/me/addresses/{id}` | **Deactivate** (not physical delete). 404 if not owned. |

### 10.4 Public catalog

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/v1/categories` | no | Active categories. |
| GET | `/api/v1/products` | no | Search/list **active** products. Query: text, categoryId, page, size. ILIKE/indexed SQL. |
| GET | `/api/v1/products/{id}` | no | Active product. Inactive or unknown → **404**. |

### 10.5 Admin catalog (`ADMIN`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/admin/categories` | Create category. |
| PUT | `/api/v1/admin/categories/{id}` | Update category. |
| DELETE | `/api/v1/admin/categories/{id}` | **Deactivate** (preserve history). |
| POST | `/api/v1/admin/products` | Create product (optional image via `ProductImageStoragePort`). |
| PUT | `/api/v1/admin/products/{id}` | Update product (optional image). |
| DELETE | `/api/v1/admin/products/{id}` | **Deactivate** (preserve history). |

Physical DELETE of catalog rows is forbidden when historical orders reference them.

### 10.6 Shopping (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/cart` | Get or create the active cart. |
| POST | `/api/v1/cart/items` | Add/increase line (`productId`, `quantity`). Revalidates product via `ProductCatalogPort`. |
| PUT | `/api/v1/cart/items/{productId}` | Set quantity. |
| DELETE | `/api/v1/cart/items/{productId}` | Remove line. |
| GET | `/api/v1/shopping-lists` | List lists (pagination defaults apply). |
| POST | `/api/v1/shopping-lists` | Create list (name; items may be included as nested resource data). |
| PUT | `/api/v1/shopping-lists/{id}` | Update owned list (name and/or nested items). |
| DELETE | `/api/v1/shopping-lists/{id}` | Delete/deactivate owned list. |

### 10.7 Checkout and customer orders (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/checkout` | Checkout. Header `Idempotency-Key`. Body: `addressId`, `paymentMethod`, explicit accepted prices per product, non-sensitive simulated-card outcome when method is `SIMULATED_CARD`. See §8. |
| GET | `/api/v1/orders` | Current user’s orders (paginated). |
| GET | `/api/v1/orders/{id}` | Own order; 404 if not owned. Includes item snapshots, address snapshot, payment summary. |
| POST | `/api/v1/orders/{id}/cancel` | Cancel if `PENDING` and within 15 minutes. 409 otherwise. |

**Not exposed:** public payment, inventory, stock, or order-payment resources.

### 10.8 Admin orders (`ADMIN`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/orders` | List orders (paginated). |
| GET | `/api/v1/admin/orders/{id}` | Order detail. |
| PATCH | `/api/v1/admin/orders/{id}/status` | Adjacent allowed transition only (`CONFIRMED → PREPARING → READY → DELIVERED`). 409 on illegal jump. Admins do not use this to cancel; customer cancel remains `PENDING → CANCELLED`. |

### 10.9 Assistant (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/assistant/chat` | Chat. Identity from JWT. Body: message text and optional confirmation payload for checkout. Must not accept a target `userId`. High-impact purchase requires explicit confirmation. |

MCP is not part of this REST contract. Transport is deferred.

---

## 11. AI / RAG / MCP

```text
User → REST /api/v1/assistant/chat
     → Assistant Chat use case
     → LLMPort
     → tool selection
     → ExecuteApprovedTool
     → existing application use case
     → domain
     → infrastructure
```

```text
Assistant
  → SearchKnowledge
    → EmbeddingPort
    → KnowledgeVectorStorePort
      → PostgreSQL + pgvector
```

Never: AI or MCP → JPA / repository / SQL of another module.

| LLM may use | Must come from business use cases / tools |
|---|---|
| User message, conversation history | Current price, stock, availability |
| Retrieved knowledge snippets | Cart, orders, payments |
| Tool results from this turn | Authenticated identity (never from the model) |

**Confirmation required** before final purchase / checkout (and other irreversible payment effects). The model cannot grant permission or skip confirmation.

**Prompt injection:** user text and knowledge chunks are untrusted data. Allowlisted tools, schema validation, auth, and confirmation are the control plane.

**MCP:** `com.superfercho.mcp` tools validate arguments, use `CurrentUserProvider` (once MCP auth is chosen), and call existing use cases. No duplicated business rules. Exact MCP transport is deferred.

---

## 12. Scheduling (automatic confirmation)

- Trigger only: `orders.infrastructure.scheduling` with Spring `@Scheduled`.
- Poll interval is configuration, not a business rule.
- The method only calls `AutoConfirmPendingOrders`.
- Use case uses injectable `Clock` (UTC): confirm `PENDING` orders whose 15-minute window has elapsed.
- Skip `CANCELLED` and already `CONFIRMED`. Idempotent conditional update (`WHERE status = 'PENDING'`).
- One transaction per order.
- Tests use `Clock.fixed(...)`. No `Thread.sleep` to express the rule.
- Cancel vs confirm race: one conditional update wins.

---

## 13. Persistence strategy

| Aggregate | Domain / JPA separation | Why |
|---|---|---|
| `User` + `Address` | Separate | Password hash, uniqueness, default-address invariant |
| `Product` | Separate | Stock invariants + atomic SQL |
| `Category` | Simple adapter | CRUD/status; still no JPA on domain |
| `Cart` / `ShoppingList` | Separate | Collection invariants, one active cart |
| `Order` | Separate | Lifecycle, snapshots, cancellation |
| `Payment` | Thin adapter | Few invariants; no JPA on domain; no extra mapper stack |
| Knowledge document/chunk | Simple + vector adapter | `VECTOR` is infrastructure |
| `Conversation` | Simple | Ownership; little other domain behavior |

Do not generate a mapper per table. Map in the repository adapter when separation is justified.

---

## 14. Test strategy

Preserve the pyramid: domain unit tests → application tests with mocked ports → PostgreSQL/Testcontainers integration → controller/security tests → a few critical E2E flows.

No H2. No real payment, LLM, or embedding providers. Do not chase 100% coverage.

### Critical checkout / inventory tests

- successful checkout (identity from JWT, owned active address, snapshots, stock down, cart cleared, payment recorded)
- simulated card decline (no order, stock unchanged, cart intact)
- price change without explicit acceptance (409, no charge)
- insufficient stock
- concurrent stock race (never negative stock)
- cancellation restores stock
- `APPROVED` simulated card cancel → payment `REFUNDED`
- COD: payment `PENDING`, cancel does not invent `PAID`/`REFUNDED`
- idempotency: same key+request replay; same key+different request 409; concurrent duplicate key
- auto-confirm after 15 minutes via fixed `Clock`
- public inactive product 404
- ownership: customer cannot read another user’s order/cart/address
- ADMIN cannot be obtained via public register

Application tests for shopping lists must still cover moving list items into the cart **without bypassing cart validation**, even though that is not a dedicated REST path.

---

## 15. Configuration

No secrets in Git.

**`application.yml`:** application name; `ddl-auto` validate/none; `open-in-view: false`; Flyway enabled; pagination defaults; `superfercho.orders.cancellation-window: 15m`; auto-confirm poll interval; JWT expiration ~15m; COP as the only currency.

**`application-local.yml`:** local JDBC via env; scheduler on; filesystem image directory.

**`application-test.yml`:** Testcontainers PostgreSQL; scheduler off unless a test enables it; fake LLM/embedding; no real providers.

**Environment placeholders:** `SUPERFERCHO_DB_URL`, `SUPERFERCHO_DB_USERNAME`, `SUPERFERCHO_DB_PASSWORD`, `SUPERFERCHO_JWT_SECRET`, later `SUPERFERCHO_LLM_*` / `SUPERFERCHO_EMBEDDING_*` when those phases start, `SUPERFERCHO_STORAGE_DIRECTORY` for local images.

---

## 16. Maven dependencies

**Required when the corresponding phase exists:**

- `spring-boot-starter-web`, `validation`, `data-jpa`, `security`
- Flyway + PostgreSQL
- PostgreSQL JDBC
- JWT support (Nimbus / Spring Security JWT)
- `spring-boot-starter-test`, `spring-security-test`
- Testcontainers PostgreSQL; pgvector image when Knowledge tests run
- Hibernate vector module **only when Knowledge starts**
- JDK `HttpClient` for LLM/embedding HTTP when those phases start

**Not introduced:** Lombok, LangChain, LangGraph, Spring AI as an orchestration framework, Redis, Kafka, RabbitMQ, Elasticsearch, MapStruct (unless later mapping volume justifies it), H2, cloud storage SDKs.

**Optional later:** springdoc, Actuator, ArchUnit.

Exact Spring Boot 3.x minor and PostgreSQL/pgvector versions are deferred.

---

## 17. Implementation order

1. **Phase 0 — foundation:** Maven wrapper, Spring Boot, Flyway, PostgreSQL, UTC `Clock`, RFC 7807 error mapping, `Money` (COP), Testcontainers smoke
2. **Identity:** User/address, register/login, JWT (~15 min), `CurrentUserProvider`, bootstrap admin, ownership tests
3. **Catalog:** Category 1→N Product, public 404 for inactive, admin deactivate-on-DELETE, stock decrement/restore, local image adapter, concurrency tests
4. **Shopping:** Cart/lists, `ProductCatalogPort`
5. **Payments:** Payment aggregate + simulator behind application API (no public controller)
6. **Orders / checkout:** Ports, one-TX checkout, price acceptance, idempotency 24h, cancel+refund, scheduler auto-confirm
7. **Knowledge:** documents, ports, pgvector (provider/dimension/chunking chosen in that phase)
8. **Assistant:** `POST /api/v1/assistant/chat`, `LLMPort` fake in tests, tools, confirmation
9. **MCP:** thin tools; transport chosen in that phase
10. **Hardening:** remaining security tests, secret audit, checkout E2E

Do not start Assistant before Catalog/Shopping/Orders application contracts exist.

---

## 18. Architectural risks

| Risk | Impact | Mitigation |
|---|---|---|
| Checkout TX split | Partial purchase | One `@Transactional` on `Checkout`; payment joins caller TX |
| Non-atomic stock | Oversell | `UPDATE … WHERE stock >= qty`; concurrency tests |
| Idempotency without lock | Duplicate orders | Unique `(user_id, key)` + same TX |
| Cancel vs auto-confirm | Double process | Conditional `PENDING` update |
| Cross-module JPA | Broken ownership | Logical UUIDs; consumer ports only |
| RAG used as price/stock | Wrong charges | Tools + tests |
| LLM impersonation | Wrong user’s cart | Strip client/model user ids; JWT actor |
| Public payment API | Leaked simulator | No payment resource |
| Physical catalog DELETE | Broken order history | HTTP DELETE = deactivate |

---

## 19. Decisions: resolved vs deferred

### 19.1 Resolved (integrated throughout this document)

- REST base `/api/v1` and the endpoint list in §10 (authoritative, not candidate)
- Pagination `page=0`, `size=20`, max `100`
- Public inactive product = 404
- RFC 7807 / `application/problem+json` with optional application `code`
- HTTP 400/401/403/404/409/500 semantics
- One `User` aggregate, one role, no self-register ADMIN, bootstrap ADMIN, ADMIN not a shopper
- Address fields; deactivate not physical delete; snapshot on order
- COP only; `Money` in minimal platform
- Category 1→N Product; product fields including brand and image reference
- `ProductImageStoragePort` + local filesystem; no cloud store
- Payment statuses **PENDING / APPROVED / DECLINED / REFUNDED**; COD stays PENDING; card cancel → REFUNDED; `PAID` deferred
- Checkout one local TX; explicit price acceptance → 409 on change
- Idempotency-Key: same request replay; different request 409; 24h success retention
- Adjacent-only order transitions
- JWT access ~15 minutes; no refresh
- PostgreSQL schemas per module; logical cross-module UUIDs
- Single Maven module
- Search: PostgreSQL ILIKE; no Elasticsearch
- UTC `Clock`
- AI/MCP boundaries unchanged in intent

### 19.2 Deferred (do not invent now)

- Specific LLM provider
- Specific embedding provider/model
- Embedding dimension
- Exact knowledge chunking algorithm
- Exact MCP transport and MCP authentication binding
- Cloud image provider (explicitly out of MVP)
- PostgreSQL full-text search or `pg_trgm` (only if ILIKE proves insufficient)
- JWT refresh / rotation / revocation
- Exact Spring Boot 3.x minor, PostgreSQL major, and pgvector version
- Knowledge HTTP API (not in the MVP REST contract)
- COD `PAID` at delivery / cash collection
- Persistent idempotency `FAILED`/`RECOVERING` states

### 19.3 Alignment with `.cursor/rules/`

Payment statuses in `.cursor/rules/00-global-architecture.mdc` §22 match this blueprint: `PENDING`, `APPROVED`, `DECLINED`, `REFUNDED`. `PAID` is not an MVP status.

No remaining payment-status contradiction with the architecture rules.

---

## 20. Final verdict

The blueprint is internally consistent as an implementation specification. Payment statuses are aligned with `.cursor/rules/00-global-architecture.mdc`.

Remaining work is the deferred phase choices listed in §19.2.

**READY FOR PHASE 0 IMPLEMENTATION**
