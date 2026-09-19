# SuperFercho Backend Technical Architecture Blueprint

**Status:** Architectural reference of the implemented system (MVP closed)
**Type:** Documentation only
**Baseline:** Modular monolith, Clean / Hexagonal Architecture  
**Source of truth:** Application source and Flyway migrations. This document describes that system; it does not override it.

---

## 0. Baseline tecnológico y módulos

| Concern | Implemented decision |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Build | Maven, single module, Maven Wrapper |
| Architecture | Modular monolith, Clean / Hexagonal |
| Database | One PostgreSQL database |
| Knowledge vectors | pgvector in that same PostgreSQL database |
| Persistence | Spring Data JPA / Hibernate; `ddl-auto: none`; `open-in-view: false`; Hibernate JDBC timezone UTC |
| Schema evolution | Flyway (`V1`–`V9`) |
| API | REST under `/api/v1` |
| Security | Spring Security, JWT Bearer access tokens (~15 minutes) |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, Testcontainers PostgreSQL |
| Integration-test DB | PostgreSQL (+ pgvector for Knowledge). H2 is not used |

**Not part of the system:** microservices, Kafka, RabbitMQ as internal architecture, Redis, Elasticsearch, Kubernetes, Saga, XA, distributed transactions, a second database, a second vector store, LangChain, LangGraph, or another AI orchestration framework.

**Business modules:** `identity`, `catalog`, `shopping`, `orders`, `payments`, `knowledge`, `assistant`.

**MCP** (`com.superfercho.mcp`) is a stub package, not a business module and not an operational integration layer.

---

## 1. Estructura del artefacto

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

### 1.2 Package tree per business module

REST controllers live under `infrastructure/rest`. There is no `presentation/rest` package.

```text
com.superfercho.identity
├── domain/model | exception
├── application/usecase | dto | port | exception
└── infrastructure/persistence | security | rest | configuration

com.superfercho.catalog
├── domain/model | exception
├── application/usecase | dto | port | exception
└── infrastructure/persistence | rest | configuration
    # InventoryPort implemented in persistence (InventoryPersistenceAdapter)

com.superfercho.shopping
├── domain/model | exception
├── application/service | dto | port/in | port/out | exception
└── infrastructure/persistence | catalog | identity | rest | configuration

com.superfercho.orders
├── domain/model | exception
├── application/usecase | dto | port | exception
└── infrastructure/persistence | shopping | identity | catalog
                              | rest | clock | configuration

com.superfercho.payments
├── domain/model | exception
├── application/usecase | dto | port | exception
└── infrastructure/persistence | integration | rest | configuration

com.superfercho.knowledge
├── domain/model | exception
├── application/usecase | dto | port | exception
└── infrastructure/persistence | rest | clock | configuration
    └── integration/embedding | vector | chunking

com.superfercho.assistant
├── domain/model | exception
├── application/service | dto | port/in | port/out | tool | confirmation | exception
└── infrastructure/rest | llm | conversation | confirmation | identity | clock | configuration

com.superfercho.mcp
└── package-info.java          # stub only; no tools, transport, or auth
```

Use cases are registered as beans in each module’s `infrastructure/configuration`. Domain has no Spring, JPA, or HTTP annotations.

### 1.3 Platform package (minimal)

```text
com.superfercho.platform
├── money                       # Money value object (COP)
├── time                        # Clock bean (UTC)
└── error                       # RFC 7807 fallback mapping
```

`Money` lives here because Catalog, Shopping, Orders, and Payments share the same monetary concept. The platform layer does not accumulate unrelated utilities.

---

## 2. Dependencias y ownership de ports

Logical direction inside one deployable JAR:

```text
REST / Assistant tools
        ↓
application use cases
        ↓
domain

infrastructure adapters implement consumer ports
and call the provider module through its application API
(use cases or application repository ports, never JPA)
```

| Consumer | Provider | Port / mechanism | Forbidden |
|---|---|---|---|
| Shopping → Catalog | Current ACTIVE price | Shopping `ProductCatalogPort` → Catalog `ProductQueryPort` (`FindProductPriceUseCase`) | Catalog JPA/repositories |
| Orders → Catalog | Sale snapshot + availability | Orders `ProductCatalogPort` | Catalog JPA |
| Orders → Catalog | Atomic decrement / restore | Catalog `InventoryPort` (provider-owned) | Catalog JPA from Orders |
| Orders → Shopping | Load / clear active cart | Orders `ShoppingCartPort` | Shopping persistence |
| Orders → Identity | Load usable address | Orders `CustomerAddressPort` | Identity JPA |
| Orders → Payments | Charge / refund simulated payment | Orders `PaymentPort` | Payments persistence |
| Assistant → business modules | Same use cases as REST (allowlisted tools) | Direct use-case calls from Assistant tools | Any other module’s DB/JPA |
| Knowledge → pgvector | Vector insert/search | `KnowledgeVectorStorePort` | Second vector database |

Provider modules do not depend on their consumers. Catalog, Identity, Payments, and Knowledge do not depend on Orders, Shopping, or Assistant.

No module uses another module’s JPA entities or Spring Data repositories. Adapter wiring is mixed: Shopping and Payments are consumed through application use cases; Orders’ catalog and address adapters currently call Catalog `ProductRepository` and Identity `AddressRepository` (application ports) and map those modules’ domain models into Orders DTOs. Assistant tools call use cases only.

`InventoryPort` is owned by **Catalog**. Orders imports that application port and calls it from checkout and cancel.

`CurrentUserProvider` is not a single global contract. Identity, Shopping, Orders, and Assistant each declare their own port. Identity’s security adapter (`SpringSecurityCurrentUserProvider`) is the JWT principal source; other modules adapt to it. Several use cases read the provider internally rather than receiving an explicit `Actor` argument.

Domain objects do not read Spring `SecurityContext`.

---

## 3. Modelo de dominio

Invariants belong on aggregates. Checkout is an application use case.

### 3.1 Identity

There is no separate Customer or Admin aggregate. A user has exactly one role: `CUSTOMER` or `ADMIN`.

Public registration (`RegisterCustomerUseCase`) creates `CUSTOMER` only. Users cannot self-register as `ADMIN`. There is no public profile endpoint and no bootstrap-admin use case in the runtime API.

`ADMIN` is not a shopper: cart, lists, checkout, and assistant chat require `CUSTOMER`.

| Kind | Name |
|---|---|
| Aggregate | `User` |
| Aggregate | `Address` (owned by a user; separate repository) |
| VO / enums | `Role`, `UserStatus`, `AddressStatus` |
| Repository | `UserRepository`, `AddressRepository` |

**User fields:** `id` (UUID), `documentType`, `documentNumber`, `fullName`, `email`, `phone`, `passwordHash`, `role`, `status`, timestamps.

`documentType` is a business string. There are no Colombian tax, RUT/NIT, or check-digit rules.

**Address fields:** `label`, `recipientName`, `addressLine`, `additionalInfo`, `city`, `department`, `phone`, `isDefault`, `status`, timestamps.

Rules:

- `email` unique; `documentType` + `documentNumber` unique
- multiple addresses per user
- at most one **active** default address
- addresses are deactivated, not physically deleted
- inactive addresses cannot be selected for checkout
- orders store an immutable shipping-address snapshot (not a live address row)

### 3.2 Catalog

| Kind | Name |
|---|---|
| Aggregate | `Product` |
| Aggregate | `Category` |
| Enums | `ProductStatus`, `CategoryStatus` (`ACTIVE`, `INACTIVE`) |
| Repository | `ProductRepository`, `CategoryRepository` |

**Category 1 → N Products.** A product has exactly one main category.

**Product fields:** `id`, `categoryId`, optional unique barcode, `name`, `brand` (nullable), `description`, `price` (`Money`, COP), `stock`, `imageUrl` (nullable string), `status`, timestamps.

There is no image-storage port. The product stores a URL string.

Rules:

- `price >= 0`, `stock >= 0`
- `stock = 0` means unavailable
- inactive products cannot be publicly purchased or publicly fetched (public GET → 404)
- inactive categories cannot be used publicly
- Catalog owns stock
- historical rows are not physically deleted
- activation/deactivation are explicit use cases (`Activate*` / `Deactivate*`), not HTTP DELETE

Public catalog reads default to `CatalogView.PUBLIC`. Admin listing uses `view=ADMIN`.

### 3.3 Shopping

| Kind | Name |
|---|---|
| Aggregate | `Cart` |
| Entity | `CartItem` |
| Aggregate | `ShoppingList` |
| Entity | `ShoppingListItem` |
| Enum | `CartStatus` (`ACTIVE` only) |

- one cart per customer (`UNIQUE` on `customer_id`)
- one product line per product in a cart (domain invariant; Flyway does not declare `UNIQUE (cart_id, product_id)`)
- quantity > 0
- stock is not reserved when adding to cart or lists
- cart stored price (`price_at_addition`) is informational; Catalog current price is authoritative at checkout
- adding a product requires an ACTIVE catalog product via `ProductQueryPort`; inactive/missing → not found
- `AddShoppingListToCart` is an application/tool capability; there is no dedicated REST path

### 3.4 Orders

| Kind | Name |
|---|---|
| Aggregate | `Order` |
| Entity | `OrderItem` |
| VO | `OrderNumber`, `ShippingAddressSnapshot` |
| Enum | `OrderStatus` |
| Repository | `OrderRepository` |

**Order states:** `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`.

**Valid transitions only:**

```text
PENDING    → CONFIRMED
PENDING    → CANCELLED
CONFIRMED  → PREPARING
PREPARING  → READY
READY      → DELIVERED
```

Customer cancellation: only while `PENDING` and within 15 minutes of `createdAt` (`Order.CUSTOMER_CANCELLATION_WINDOW`). Cancellation restores stock. If the linked payment is `APPROVED`, Payments records a refund via `refundedAt`; payment status stays `APPROVED`. COD payments remain `PENDING` and are not refunded.

After 15 minutes, remaining `PENDING` orders are auto-confirmed (`PENDING → CONFIRMED`). Customers then cannot cancel.

`order_number` is assigned at creation (`ORD-` + fragment of the order UUID). Idempotency storage is application/infrastructure owned by Orders, not a domain aggregate.

Admin status updates (`UpdateOrderStatusUseCase`) apply adjacent transitions `CONFIRMED → PREPARING → READY → DELIVERED` (and may confirm `PENDING → CONFIRMED`). They do not cancel.

### 3.5 Payments

| Kind | Name |
|---|---|
| Aggregate | `Payment` |
| Enums | `PaymentMethod`, `PaymentStatus` |
| Repository | `PaymentRepository` |

**Methods:** `SIMULATED_CARD`, `CASH_ON_DELIVERY`.

**Statuses:** `PENDING`, `APPROVED`, `DECLINED`.

There is no `REFUNDED` status. A refund of an `APPROVED` payment sets `refundedAt` and leaves status `APPROVED`.

`PAID` is not a status.

Card number, CVV, expiration date, and payment credentials are not stored.

**Runtime simulator (`ProcessPaymentUseCase`):**

| Method | Checkout result |
|---|---|
| `SIMULATED_CARD` | always `APPROVED`; `provider_reference = sim-approved`; order `PENDING` |
| `CASH_ON_DELIVERY` | `PENDING`; `provider_reference = cod-pending`; order `PENDING` |

`DECLINED` exists on the enum and in the PostgreSQL CHECK. The current simulator does not produce it. Checkout still rejects a `DECLINED` result if one were returned (`PaymentDeclinedException`).

**Cancel mapping:**

| Method | Customer cancel in window |
|---|---|
| `SIMULATED_CARD` (`APPROVED`) | `refundedAt` set; status remains `APPROVED`; stock restored |
| `CASH_ON_DELIVERY` (`PENDING`) | no `refundedAt`; status remains `PENDING`; stock restored |

There is no external refund provider.

### 3.6 Knowledge

| Kind | Name |
|---|---|
| Aggregate | `KnowledgeDocument` |
| Entity | `KnowledgeChunk` |
| Enum | `DocumentStatus`: `RECEIVED`, `CHUNKED`, `READY`, `FAILED`, `INACTIVE` |
| Repository | `KnowledgeDocumentRepository` |

Embeddings are infrastructure behind `KnowledgeVectorStorePort`. Chunking is `DocumentChunkerPort` (`CharacterOverlapDocumentChunker`). Embedding vendor is `EmbeddingPort` (`OpenAiEmbeddingAdapter`, model `text-embedding-3-small`, dimension 1536).

RAG is informational. It is not the source of truth for stock, price, cart, orders, payments, or identity.

### 3.7 Assistant

| Kind | Name |
|---|---|
| Aggregate | `Conversation` |
| Entity | `Message` |
| Enums | `MessageRole`; sensitive types `CHECKOUT`, `CANCEL_ORDER` |

Conversations are persisted in process memory (`ConversationStore` → `InMemoryConversationStore`). There is no Assistant schema and no SQL tables.

Assistant domain does not own Product, Cart, Order, or Payment types. The public REST surface is `POST /api/v1/assistant/chat`.

---

## 4. Casos de uso implementados

### Identity

- `RegisterCustomerUseCase` — public; role `CUSTOMER` only
- `AuthenticateUserUseCase`
- `ListAddressesUseCase`, `AddAddressUseCase`, `UpdateAddressUseCase`, `DeactivateAddressUseCase`, `SetDefaultAddressUseCase`

There is no `GetCurrentUser` use case and no public admin-provisioning use case.

### Catalog

- `CreateCategoryUseCase`, `UpdateCategoryUseCase`, `ActivateCategoryUseCase`, `DeactivateCategoryUseCase`, `ListCategoriesUseCase`, `GetCategoryUseCase`
- `CreateProductUseCase`, `UpdateProductUseCase`, `ActivateProductUseCase`, `DeactivateProductUseCase`, `ChangeProductPriceUseCase`
- `GetProductUseCase`, `ListProductsUseCase`, `SearchProductsUseCase`
- `FindProductPriceUseCase` — implements `ProductQueryPort` (ACTIVE only)
- `InventoryPort.decreaseStockAtomically` / `restoreStock`

### Shopping

- `GetCartUseCase`, `AddProductToCartUseCase`, `ChangeCartItemQuantityUseCase`, `RemoveProductFromCartUseCase`, `ClearCartUseCase`
- `CreateShoppingListUseCase`, `RenameShoppingListUseCase`, `ListShoppingListsUseCase`, `GetShoppingListUseCase`
- `AddProductToShoppingListUseCase`, `ChangeShoppingListItemQuantityUseCase`, `RemoveProductFromShoppingListUseCase`, `ClearShoppingListUseCase`
- `AddShoppingListToCartUseCase` — application/tool; same cart validation as `AddProductToCartUseCase`

There is no REST delete of a shopping list.

### Orders

- `CheckoutUseCase` — wrapped by `TransactionalCheckoutUseCase`
- `ListOrdersUseCase`, `GetOrderUseCase` — authenticated customer; ownership enforced
- `CancelOrderUseCase` — wrapped by `TransactionalCancelOrderUseCase`
- `UpdateOrderStatusUseCase` — HTTP restricted to `ADMIN`
- `AutoConfirmPendingOrdersUseCase`

### Payments

- `ProcessPaymentUseCase` — local simulator; joins caller transaction
- `RefundPaymentUseCase` — `APPROVED` → set `refundedAt`
- `GetPaymentUseCase` — `GET /api/v1/payments/{paymentId}` for `ADMIN`

### Knowledge

- `CreateDocumentUseCase`, `GetDocumentUseCase`, `ListDocumentsUseCase`
- `ReplaceDocumentContentUseCase`, `ProcessDocumentUseCase`
- `DeactivateDocumentUseCase`, `ReactivateDocumentUseCase`
- `SearchKnowledgeUseCase`

HTTP for Knowledge is implemented and restricted to `ADMIN`. Assistant retrieval uses `SearchKnowledgeUseCase`.

### Assistant

- `ChatUseCase` / `ChatApplicationService` — identity from Assistant `CurrentUserProvider`
- Allowlisted tools; explicit confirmation for `checkout` and `cancel_order`

### MCP

No business use cases. No operational tools.

---

## 5. Contratos entre módulos

### Catalog `ProductQueryPort`

- **Owner:** Catalog
- **Implementation:** `FindProductPriceUseCase`
- **Purpose:** Operational lookup of an ACTIVE product’s current COP price
- **Output:** empty when missing or not `ACTIVE`

### Shopping `ProductCatalogPort`

- **Owner:** Shopping
- **Purpose:** Current price for cart/list writes
- **Adapter:** `shopping.infrastructure.catalog.ProductCatalogAdapter` → `ProductQueryPort`

### Orders `ProductCatalogPort`

- **Owner:** Orders
- **Purpose:** Name, ACTIVE flag, stock availability, current price; availability check for checkout lines
- **Adapter:** `orders.infrastructure.catalog.ProductCatalogAdapter` → Catalog `ProductRepository` (maps Catalog `Product` to Orders `ProductCatalogInfo`)

Not a duplicate of Shopping’s port. Different shape, different consumer. `checkAvailability` compares requested quantity to catalog stock. ACTIVE/saleability is enforced when building order items from `getProduct`.

### Catalog `InventoryPort`

- **Owner:** Catalog
- **Purpose:** Atomic decrement; restore on cancel
- **Input:** `(productId, quantity)` lines
- **Implementation:** `InventoryPersistenceAdapter` / native SQL
- **Consumer:** Orders checkout and cancel

Shopping does not decrement stock.

### Orders `ShoppingCartPort`

- **Owner:** Orders
- **Purpose:** Load the authenticated customer’s cart; clear after successful checkout
- **Adapter:** `orders.infrastructure.shopping.ShoppingCartAdapter` → Shopping `GetCartUseCase` / `ClearCartUseCase`

### Orders `CustomerAddressPort`

- **Owner:** Orders
- **Purpose:** Load an active address owned by the customer
- **Adapter:** `orders.infrastructure.identity.CustomerAddressAdapter` → Identity `AddressRepository` (owned + `ACTIVE`)

### Orders `PaymentPort`

- **Owner:** Orders
- **Purpose:** Simulated charge; load payment; refund approved card via `refundedAt`
- **Adapter:** `payments.infrastructure.integration.PaymentIntegrationAdapter` → Payments use cases

No PAN/CVV. Simulator is deterministic: CARD → `APPROVED`, COD → `PENDING`.

### Orders `IdempotencyPort`

- **Owner:** Orders
- **Purpose:** Validate reuse of `(customerId, idempotencyKey)` and persist fingerprint + materialized checkout result (24h retention)

### `CurrentUserProvider`

- Identity: `identity.application.port.CurrentUserProvider` → `SpringSecurityCurrentUserProvider`
- Shopping, Orders, Assistant: consumer-owned ports + identity adapters
- Output: authenticated `userId` (`UUID`)
- Authorization roles come from the JWT security filter, not from this port

### Knowledge ports

| Port | Owner | Adapter |
|---|---|---|
| `KnowledgeVectorStorePort` | Knowledge | `PgVectorKnowledgeStoreAdapter` |
| `EmbeddingPort` | Knowledge | `OpenAiEmbeddingAdapter` (tests: fake) |
| `DocumentChunkerPort` | Knowledge | `CharacterOverlapDocumentChunker` |

### Assistant `LLMPort`

- **Owner:** Assistant
- **Adapter:** `OpenAiChatAdapter`
- Tests use a deterministic fake

There is no `ProductImageStoragePort`.

---

## 6. Persistencia

One PostgreSQL database. Flyway is authoritative. `spring.jpa.hibernate.ddl-auto` is `none`.

**Schemas present:** `identity`, `catalog`, `shopping`, `orders`, `payments`, `knowledge`.

There is no `assistant` schema.

One DataSource, one local transaction manager.

**Cross-module references:** logical UUIDs, no physical foreign keys.
**Intra-module relationships:** PostgreSQL foreign keys, unique constraints, and CHECKs.

Technical keys: UUID. Monetary amounts: `NUMERIC(12,2)` with `currency CHAR(3)` CHECK (`currency = 'COP'`). Canonical time: UTC `TIMESTAMPTZ`.

### 6.1 V1 — pgvector

`CREATE EXTENSION IF NOT EXISTS vector;`

No business tables.

### 6.2 V2 — Identity

**`identity.users`**

- `id` UUID PK
- `document_type` VARCHAR(50) NOT NULL
- `document_number` VARCHAR(50) NOT NULL
- UNIQUE (`document_type`, `document_number`)
- `full_name` VARCHAR(255) NOT NULL
- `email` VARCHAR(255) NOT NULL UNIQUE
- `phone` VARCHAR(50) NOT NULL
- `password_hash` VARCHAR(255) NOT NULL
- `role` VARCHAR(20) NOT NULL CHECK (`CUSTOMER`, `ADMIN`)
- `status` VARCHAR(20) NOT NULL CHECK (`ACTIVE`, `INACTIVE`)
- `created_at` / `updated_at` TIMESTAMPTZ NOT NULL

**`identity.addresses`**

- `id` UUID PK
- `user_id` UUID NOT NULL FK → `identity.users.id`
- `label`, `recipient_name`, `address_line`, `city`, `department`, `phone` NOT NULL
- `additional_info` VARCHAR(255) NULL
- `is_default` BOOLEAN NOT NULL
- `status` VARCHAR(20) NOT NULL CHECK (`ACTIVE`, `INACTIVE`)
- timestamps
- Partial unique: one active default per user  
  `uk_identity_addresses_one_active_default` on `(user_id) WHERE is_default = true AND status = 'ACTIVE'`
- Index `idx_identity_addresses_user_id`

### 6.3 V3 — Catalog

**`catalog.categories`**

- `id` UUID PK
- `name` VARCHAR(255) NOT NULL
- `description` TEXT NULL
- `status` VARCHAR(20) NOT NULL CHECK (`ACTIVE`, `INACTIVE`)
- timestamps
- Index on `status`

**`catalog.products`**

- `id` UUID PK
- `category_id` UUID NOT NULL FK → `catalog.categories.id`
- `barcode` VARCHAR(64) NULL; unique where NOT NULL
- `name` VARCHAR(255) NOT NULL
- `brand` VARCHAR(255) NULL
- `description` TEXT NULL
- `price_amount` NUMERIC(12,2) NOT NULL CHECK (`>= 0`)
- `currency` CHAR(3) NOT NULL CHECK (`= 'COP'`)
- `stock` INTEGER NOT NULL CHECK (`>= 0`)
- `image_url` VARCHAR(1024) NULL
- `status` VARCHAR(20) NOT NULL CHECK (`ACTIVE`, `INACTIVE`)
- timestamps
- Indexes: `category_id`, `status`, `(category_id, status)`

There is no `version` column and no `image_uri` column.

Atomic decrement:

```text
UPDATE catalog.products
   SET stock = stock - :quantity,
       updated_at = :updatedAt
 WHERE id = :id
   AND status = 'ACTIVE'
   AND stock >= :quantity
   AND :quantity > 0
```

Zero rows → insufficient stock. Restore uses `stock = stock + :quantity` without requiring `ACTIVE`.

### 6.4 V4 — Orders

**`orders.orders`**

- `id` UUID PK
- `order_number` VARCHAR(64) NOT NULL UNIQUE
- `customer_id` UUID NOT NULL (logical)
- `status` VARCHAR(20) NOT NULL CHECK (`PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`)
- `subtotal_amount` / `total_amount` NUMERIC(12,2) NOT NULL CHECK (`>= 0`)
- `subtotal_currency` / `total_currency` CHAR(3) NOT NULL CHECK (`= 'COP'`)
- `payment_id` UUID NULL (logical)
- shipping snapshot: `shipping_recipient_name`, `shipping_address_line`, `shipping_additional_info`, `shipping_city`, `shipping_department`, `shipping_phone`
- `created_at` TIMESTAMPTZ NOT NULL
- `confirmed_at` / `cancelled_at` NULL
- `updated_at` TIMESTAMPTZ NOT NULL
- Indexes: `customer_id`, `status`, `created_at`

There is no `payment_method`, `cancellation_deadline_at`, `placed_at`, `version`, or `user_id` column. The 15-minute window is `created_at + 15 minutes` in domain.

**`orders.order_items`**

- `id` UUID PK
- `order_id` UUID NOT NULL FK → `orders.orders.id` ON DELETE CASCADE
- `item_index` INTEGER NOT NULL CHECK (`>= 0`)
- `product_id` UUID NOT NULL (logical)
- `product_name` VARCHAR(255) NOT NULL
- `unit_price_amount` NUMERIC(12,2) NOT NULL
- `unit_price_currency` CHAR(3) NOT NULL CHECK (`= 'COP'`)
- `quantity` INTEGER NOT NULL CHECK (`> 0`)
- `subtotal_amount` / `subtotal_currency`

### 6.5 V5 — Shopping

**`shopping.carts`**

- `id` UUID PK
- `customer_id` UUID NOT NULL UNIQUE (logical)
- `status` VARCHAR(20) NOT NULL CHECK (`ACTIVE`)
- timestamps

**`shopping.cart_items`**

- `id` UUID PK
- `cart_id` UUID NOT NULL FK → `shopping.carts.id` ON DELETE CASCADE
- `item_index` INTEGER NOT NULL
- `product_id` UUID NOT NULL (logical)
- `quantity` INTEGER NOT NULL CHECK (`> 0`)
- `price_at_addition_amount` NUMERIC(12,2) NOT NULL
- `price_at_addition_currency` CHAR(3) NOT NULL CHECK (`= 'COP'`)
- `added_at` / `updated_at`

**`shopping.shopping_lists` / `shopping.shopping_list_items`**

- list: `id`, `customer_id` (logical), `name`, timestamps
- items: FK to list ON DELETE CASCADE, `item_index`, `product_id` (logical), `quantity` > 0

### 6.6 V6 — Payments

**`payments.payments`**

- `id` UUID PK
- `order_id` UUID NOT NULL (logical)
- `amount` NUMERIC(12,2) NOT NULL CHECK (`>= 0`)
- `currency` CHAR(3) NOT NULL CHECK (`= 'COP'`)
- `payment_method` VARCHAR(20) NOT NULL CHECK (`SIMULATED_CARD`, `CASH_ON_DELIVERY`)
- `status` VARCHAR(20) NOT NULL CHECK (`PENDING`, `APPROVED`, `DECLINED`)
- `provider_reference` VARCHAR(255) NULL
- `created_at` / `updated_at` TIMESTAMPTZ NOT NULL
- `refunded_at` TIMESTAMPTZ NULL

No PAN, CVV, or expiry columns.

### 6.7 V7 — Checkout idempotency

**`orders.checkout_idempotency`**

- `id` UUID PK
- `customer_id` UUID NOT NULL
- `idempotency_key` TEXT NOT NULL
- UNIQUE (`customer_id`, `idempotency_key`)
- `fingerprint` TEXT NOT NULL
- materialized result: `result_order_id`, `result_order_number`, `result_order_status`, `result_payment_status`, `result_total_amount`, `result_total_currency`
- `created_at` / `expires_at` TIMESTAMPTZ NOT NULL
- Index on `expires_at`

Retention in Application is 24 hours (`CheckoutUseCase.IDEMPOTENCY_RETENTION`). Same key + same fingerprint → replay. Same key + different fingerprint → conflict.

### 6.8 V8 — Knowledge documents

**`knowledge.documents`**

- `id` UUID PK
- `title` VARCHAR(255) NOT NULL
- `source` VARCHAR(255) NOT NULL
- `content` TEXT NOT NULL
- `status` VARCHAR(20) NOT NULL CHECK (`RECEIVED`, `CHUNKED`, `READY`, `FAILED`, `INACTIVE`)
- timestamps

**`knowledge.document_chunks`**

- `id` UUID PK
- `document_id` UUID NOT NULL FK → `knowledge.documents.id` ON DELETE CASCADE
- `position` INTEGER NOT NULL CHECK (`>= 0`)
- UNIQUE (`document_id`, `position`)
- `text` TEXT NOT NULL
- `embedded` BOOLEAN NOT NULL

### 6.9 V9 — Embeddings

**`knowledge.document_embeddings`**

- `chunk_id` UUID PK
- `document_id` UUID NOT NULL FK → `knowledge.documents.id` ON DELETE CASCADE
- `position` INTEGER NOT NULL
- `embedding vector(1536)` NOT NULL
- HNSW index `idx_knowledge_document_embeddings_vector` using `vector_cosine_ops`

---

## 7. Fronteras transaccionales

Transactions are started in Infrastructure wrappers via `TransactionTemplate`, not with `@Transactional` on Application use cases. Domain does not manage transactions. Single local PostgreSQL transaction. No Saga, XA, brokers, or outbox.

| Use case | Boundary |
|---|---|
| Register / address mutations | One TX; unique and partial-unique constraints enforce invariants |
| Cart / shopping-list writes | One TX on shopping tables; catalog is read via port; no stock write |
| **Checkout** | `TransactionalCheckoutUseCase` wraps `CheckoutUseCase.execute` in one local TX (idempotency, stock, payment, order, cart clear) |
| `ProcessPaymentUseCase` | Joins the caller TX |
| **Cancel** | `TransactionalCancelOrderUseCase` wraps cancel + stock restore + optional refund |
| `AutoConfirmPendingOrdersUseCase` | One persist per eligible order inside the use case loop; scheduler does not wrap the batch |

Failed checkout rolls back completely.

---

## 8. Checkout

**Endpoint:** `POST /api/v1/orders`
**Auth:** JWT, role `CUSTOMER`
**Header:** `Idempotency-Key` (Spring `required = false`; Application rejects null/blank)
**Body:** `addressId`, `paymentMethod`, `items[]` with `productId`, `quantity`, `expectedUnitPrice`
**Currency:** COP  
**Transaction:** `TransactionalCheckoutUseCase` → `TransactionTemplate` → `CheckoutUseCase`

The request item set (product ids and quantities) must match the active cart exactly. Cart display prices are not charged.

### 8.1 Identity and idempotency

1. `CurrentUserProvider` yields the customer id. Client-supplied user ids are ignored.
2. Missing/blank `Idempotency-Key` → validation error.
3. Fingerprint is computed from address, method, items, quantities, and `expectedUnitPrice`.
4. Same key + same fingerprint + unexpired record → return the materialized original result.
5. Same key + different fingerprint → conflict.

### 8.2 Inside the transaction

6. Load active cart. Empty → conflict.
7. Load active owned address. Missing / not owned / inactive → not available.
8. Match request lines to cart lines.
9. Load each product via Orders `ProductCatalogPort`. Inactive/unavailable → not available.
10. `expectedUnitPrice` must equal Catalog current price. Mismatch → price-changed conflict. The new price is not charged silently.
11. Availability check, then `InventoryPort.decreaseStockAtomically`. Concurrent checkouts cannot produce negative stock.
12. `PaymentPort.processPayment` for the draft order id and total.
    - `SIMULATED_CARD` → `APPROVED` in the current simulator.
    - `CASH_ON_DELIVERY` → `PENDING`.
    - `DECLINED` would abort and roll back; the simulator does not emit it.
13. Persist `Order` `PENDING` with item snapshots and shipping snapshot. `created_at = clock`.
14. Clear cart.
15. Persist idempotency success (`expires_at = now + 24h`).
16. Commit.

### 8.3 Failure matrix

| Failure | Persisted business state | Typical HTTP |
|---|---|---|
| Unauthenticated | none | 401 |
| Not CUSTOMER | none | 403 |
| Missing/blank Idempotency-Key | none | 400 |
| Validation | none | 400 |
| Same key, same fingerprint, prior success | original order | 201 (same `POST /orders` handler) |
| Same key, different fingerprint | none new | 409 |
| Address missing / not owned / inactive | none | 404 / conflict |
| Empty cart | none | 409 |
| Request items ≠ cart | none | 400 |
| Inactive/unavailable product | none | 409 |
| Price changed | none | 409 |
| Insufficient stock / concurrent oversell loser | none | 409 |
| Unexpected error | rollback | 500 problem+json, no stack trace |

No distributed compensation. Rollback is the compensation.

---

## 9. Seguridad

Source: `IdentitySecurityConfiguration`.

- Spring Security, stateless JWT Bearer access token.
- CSRF, HTTP Basic, form login, and logout are disabled.
- **Access token lifetime: 15 minutes** (`superfercho.security.jwt.expiration`).
- No refresh-token subsystem, rotation, or revocation infrastructure.
- Password hashing: BCrypt via `PasswordEncoder` / `BCryptPasswordHasher`.
- JWT secret from `SUPERFERCHO_JWT_SECRET`. Not hard-coded.
- Domain never accesses `SecurityContext`.
- Personalized operations use the authenticated principal. Clients do not send `userId` / `customerId` for ownership. Ownership is enforced in Application (`GetOrderUseCase`, address use cases, cart/lists).
- `ADMIN` is not required to shop and is denied customer shopping/assistant routes.

### 9.1 Public

| Matcher | Access |
|---|---|
| `POST /api/v1/auth/login` | permitAll |
| `POST /api/v1/customers` | permitAll |
| `/error` | permitAll |
| GET `/api/v1/categories/**` and `/api/v1/products/**` without `view=ADMIN` | permitAll (`CatalogGetRequestMatcher.publicView()`) |

### 9.2 ADMIN

| Matcher | Access |
|---|---|
| GET catalog with `view=ADMIN` | `hasRole(ADMIN)` |
| `POST /api/v1/categories`, `PUT /categories/{id}`, `POST …/activate`, `POST …/deactivate` | ADMIN |
| `POST /api/v1/products`, `PUT /products/{id}`, `POST …/activate`, `POST …/deactivate`, `POST …/price` | ADMIN |
| `POST /api/v1/orders/{orderId}/status` | ADMIN |
| `GET /api/v1/payments/{paymentId}` | ADMIN |
| All `/api/v1/knowledge/**` listed in the filter (documents CRUD/process/search) | ADMIN |

### 9.3 CUSTOMER

| Matcher | Access |
|---|---|
| `/api/v1/addresses`, `/api/v1/addresses/**` | CUSTOMER |
| `/api/v1/cart`, `/api/v1/cart/**` | CUSTOMER |
| `/api/v1/shopping-lists`, `/api/v1/shopping-lists/**` | CUSTOMER |
| `POST /api/v1/orders` | CUSTOMER |
| `GET /api/v1/orders`, `GET /api/v1/orders/{orderId}` | CUSTOMER |
| `POST /api/v1/orders/{orderId}/cancel` | CUSTOMER |
| `POST /api/v1/assistant/chat` | CUSTOMER |

### 9.4 Default

`anyRequest().denyAll()`.

Assistant tools inherit the JWT principal. The model cannot supply a user id to impersonate another user.

---

## 10. Superficie REST

**Base path:** `/api/v1`

Controllers are thin and call use cases. Errors use `application/problem+json` (RFC 7807) with an application `code` where handlers define one.

Pagination (orders list): `page` default **0**, `size` default **20**, maximum **100**.

Public inactive product: **404**.

Identity for ownership-sensitive operations comes from the authenticated principal only.

### 10.1 Auth and customers (public)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/login` | Authenticate. Body: email, password. Response: access token (~15 min). |
| POST | `/api/v1/customers` | Register `CUSTOMER`. |

### 10.2 Addresses (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/addresses` | List addresses of the principal. |
| POST | `/api/v1/addresses` | Create address. |
| PUT | `/api/v1/addresses/{addressId}` | Update owned address. |
| DELETE | `/api/v1/addresses/{addressId}` | Deactivate (not physical delete). |
| POST | `/api/v1/addresses/{addressId}/default` | Set default. |

### 10.3 Catalog

GET defaults to public view. `view=ADMIN` requires `ADMIN`.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/v1/categories` | public / ADMIN view | List categories |
| GET | `/api/v1/categories/{categoryId}` | public / ADMIN view | Get category |
| POST | `/api/v1/categories` | ADMIN | Create |
| PUT | `/api/v1/categories/{categoryId}` | ADMIN | Update |
| POST | `/api/v1/categories/{categoryId}/activate` | ADMIN | Activate |
| POST | `/api/v1/categories/{categoryId}/deactivate` | ADMIN | Deactivate |
| GET | `/api/v1/products` | public / ADMIN view | List (optional `categoryId`, `status`) |
| GET | `/api/v1/products/search` | public / ADMIN view | Search text |
| GET | `/api/v1/products/{productId}` | public / ADMIN view | Get; public inactive → 404 |
| POST | `/api/v1/products` | ADMIN | Create (`imageUrl` string) |
| PUT | `/api/v1/products/{productId}` | ADMIN | Update |
| POST | `/api/v1/products/{productId}/activate` | ADMIN | Activate |
| POST | `/api/v1/products/{productId}/deactivate` | ADMIN | Deactivate |
| POST | `/api/v1/products/{productId}/price` | ADMIN | Change price |

### 10.4 Shopping (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/cart` | Get or create the active cart |
| POST | `/api/v1/cart/items` | Add/increase line |
| PUT | `/api/v1/cart/items/{productId}` | Set quantity |
| DELETE | `/api/v1/cart/items/{productId}` | Remove line |
| DELETE | `/api/v1/cart` | Clear cart |
| GET | `/api/v1/shopping-lists` | List lists |
| POST | `/api/v1/shopping-lists` | Create list |
| GET | `/api/v1/shopping-lists/{shoppingListId}` | Get owned list |
| PATCH | `/api/v1/shopping-lists/{shoppingListId}` | Rename |
| POST | `/api/v1/shopping-lists/{shoppingListId}/items` | Add item |
| PATCH | `/api/v1/shopping-lists/{id}/items/{productId}` | Change item quantity |
| DELETE | `/api/v1/shopping-lists/{id}/items/{productId}` | Remove item |
| DELETE | `/api/v1/shopping-lists/{id}/items` | Clear items |

There is no HTTP delete of a shopping list.

### 10.5 Orders

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/orders` | CUSTOMER | Checkout. Header `Idempotency-Key`. Body: `addressId`, `paymentMethod`, items with `expectedUnitPrice`. |
| GET | `/api/v1/orders` | CUSTOMER | Current user’s orders (paginated) |
| GET | `/api/v1/orders/{orderId}` | CUSTOMER | Own order; 404 if not owned |
| POST | `/api/v1/orders/{orderId}/cancel` | CUSTOMER | Cancel if `PENDING` and within 15 minutes |
| POST | `/api/v1/orders/{orderId}/status` | ADMIN | Adjacent status transition |

There is no admin order list endpoint.

### 10.6 Payments (`ADMIN`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/payments/{paymentId}` | Payment by id |

### 10.7 Knowledge (`ADMIN`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/knowledge/documents` | Create document |
| GET | `/api/v1/knowledge/documents` | List |
| GET | `/api/v1/knowledge/documents/{documentId}` | Get |
| PUT | `/api/v1/knowledge/documents/{documentId}/content` | Replace content |
| POST | `/api/v1/knowledge/documents/{documentId}/process` | Chunk + embed |
| POST | `/api/v1/knowledge/documents/{documentId}/deactivate` | Deactivate |
| POST | `/api/v1/knowledge/documents/{documentId}/reactivate` | Reactivate |
| GET | `/api/v1/knowledge/search` | Vector search (`query`, `limit`) |

### 10.8 Assistant (`CUSTOMER`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/assistant/chat` | Chat. Identity from JWT. Optional `conversationId` and confirmation token. Must not accept a target `userId`. |

MCP is not part of the REST surface.

---

## 11. Assistant, Knowledge y MCP

```text
User → REST POST /api/v1/assistant/chat
     → ChatUseCase / ChatApplicationService
     → LLMPort (OpenAiChatAdapter)
     → allowlisted tools
     → existing application use cases
     → domain
     → infrastructure
```

```text
Assistant SearchKnowledgeTool
  → SearchKnowledgeUseCase
    → EmbeddingPort
    → KnowledgeVectorStorePort
      → PostgreSQL + pgvector (vector(1536), HNSW cosine)
```

Never: Assistant or MCP → JPA / repository / SQL of another module.

| LLM may use | Must come from business use cases / tools |
|---|---|
| User message, in-memory conversation history | Current price, stock, availability |
| Retrieved knowledge snippets | Cart, orders, payments |
| Tool results from this turn | Authenticated identity (never from the model) |

**Allowlisted tools:** `search_products`, `get_product`, `list_products`, `list_categories`, `get_cart`, `add_cart_item`, `change_cart_item_quantity`, `remove_cart_item`, `clear_cart`, shopping-list tools including `add_shopping_list_to_cart`, `list_orders`, `get_order`, `checkout`, `cancel_order`, `list_addresses`, `search_knowledge`.

**Confirmation required** before `checkout` and `cancel_order`. Pending tokens are stored in memory (`InMemoryPendingSensitiveActionStore`). The model cannot grant permission or skip confirmation.

Conversations: `ConversationStore` / `InMemoryConversationStore`. Lost on process restart. No SQL schema.

**Prompt injection:** user text and knowledge chunks are untrusted data. Allowlisted tools, argument validation, JWT identity, and confirmation are the control plane.

Knowledge HTTP is ADMIN ingest/search. Assistant search is the same `SearchKnowledgeUseCase`. RAG is not a commercial source of truth.

**MCP:** `com.superfercho.mcp` contains only `package-info.java`. Not operational. No transport, no MCP tools, no MCP authentication.

---

## 12. Auto-confirmación

- Trigger: `orders.infrastructure.configuration.AutoConfirmPendingOrdersJob` with Spring `@Scheduled` (fixed delay 60s, profile `!test`).
- The job only calls `AutoConfirmPendingOrdersUseCase`.
- Use case uses injectable clock (UTC): confirm `PENDING` orders whose 15-minute window from `createdAt` has elapsed.
- Skip `CANCELLED` and already `CONFIRMED`. Domain `confirm` is a conditional transition.
- Tests use a fixed clock. Cancel vs confirm race: one valid `PENDING` transition wins.

---

## 13. Estrategia JPA vs Domain

| Aggregate | Domain / JPA separation | Why |
|---|---|---|
| `User` | Separate | Password hash, uniqueness |
| `Address` | Separate | Default-address invariant, deactivate |
| `Product` | Separate | Stock invariants + atomic SQL |
| `Category` | Separate adapter | Status; no JPA on domain |
| `Cart` / `ShoppingList` | Separate | Collection invariants, one cart per customer |
| `Order` | Separate | Lifecycle, snapshots, cancellation |
| `Payment` | Separate | Status, `refundedAt`; no JPA on domain |
| Knowledge document/chunk | JPA document + JDBC/pgvector embeddings | `VECTOR` is infrastructure |
| `Conversation` | In-memory store | No JPA entity |

Mapping happens in repository adapters. There is no MapStruct.

---

## 14. Estrategia de tests

Pyramid: domain unit tests → application tests with mocked/fake ports → PostgreSQL/Testcontainers integration → controller/security tests.

No H2. Tests do not call real payment providers. LLM and embedding ports are faked in unit/application tests.

### Checkout / inventory coverage that matches runtime

- successful checkout (JWT identity, owned active address, snapshots, stock down, cart cleared, payment recorded)
- request items must match the cart
- price change without matching `expectedUnitPrice` (conflict, no charge)
- insufficient stock
- concurrent stock race (never negative stock)
- cancellation restores stock
- `APPROVED` simulated card cancel → `refundedAt` set; status remains `APPROVED`
- COD: payment `PENDING`; cancel does not set `refundedAt`
- idempotency: same key+fingerprint replay; same key+different fingerprint conflict
- auto-confirm after 15 minutes via fixed clock
- public inactive product 404
- inactive products cannot enter cart/list via `ProductQueryPort`
- ownership: customer cannot read another user’s order/cart/address
- ADMIN cannot be obtained via public register
- Knowledge REST requires ADMIN
- Assistant chat requires CUSTOMER; tools do not accept a target user id

`DECLINED` is not a productive simulator outcome. Application can still reject a declined payment result; that path is not generated by `ProcessPaymentUseCase`.

Application tests cover moving list items into the cart without bypassing cart validation, even though that is not a dedicated REST path.

---

## 15. Configuración

No secrets in Git.

**`application.yml`:**

- application name `superfercho`
- `ddl-auto: none`; `open-in-view: false`; Hibernate JDBC timezone UTC
- Flyway enabled, `classpath:db/migration`
- datasource from `SUPERFERCHO_DB_URL` / `USERNAME` / `PASSWORD`
- `superfercho.currency: COP`
- `superfercho.security.jwt.secret` / `expiration: 15m`
- Knowledge OpenAI embeddings: `OPENAI_API_KEY`, `text-embedding-3-small`, embeddings URL
- Assistant OpenAI chat: `OPENAI_API_KEY`, chat URL, model `gpt-4o-mini`, connect timeout 5s, read timeout 60s

**`application-local.yml`:** local JDBC overrides via env. No image-storage directory. No extra scheduler flags.

**`application-test.yml`:** JWT test secret; datasource/JPA/Flyway auto-config excluded for isolated unit tests. Integration tests use Testcontainers PostgreSQL.

**Environment:** `SUPERFERCHO_DB_URL`, `SUPERFERCHO_DB_USERNAME`, `SUPERFERCHO_DB_PASSWORD`, `SUPERFERCHO_JWT_SECRET`, `OPENAI_API_KEY`, optional OpenAI URL/model/timeout overrides.

---

## 16. Dependencias Maven

Declared in `pom.xml`:

- `spring-boot-starter-web`, `data-jpa`, `security`
- Flyway core + PostgreSQL Flyway
- PostgreSQL JDBC
- Nimbus JOSE JWT
- `spring-boot-starter-test`, `spring-security-test`
- Testcontainers PostgreSQL / JUnit Jupiter / Spring Boot Testcontainers

JDK `HttpClient` is used for OpenAI chat and embeddings (no extra HTTP client dependency).

**Not present:** Lombok, LangChain, LangGraph, Spring AI as an orchestration framework, Redis, Kafka, RabbitMQ, Elasticsearch, MapStruct, H2, cloud storage SDKs, Hibernate Vector module as a Maven artifact.

---

## 17. Estado del sistema

**Implemented (MVP):**

- Modular monolith with hexagonal modules listed in §0
- Identity register/login/JWT/addresses
- Catalog, stock ownership, public vs ADMIN view
- Shopping cart and lists
- Simulated payments (`SIMULATED_CARD`, `CASH_ON_DELIVERY`)
- Checkout in one local TX, cancel + stock restore, auto-confirm
- Knowledge documents, chunking, embeddings, pgvector, ADMIN REST
- Assistant chat, `LLMPort`, allowlisted tools, confirmation for checkout and cancel
- Security `denyAll` default

**Not implemented (residual facts, not a delivery plan):**

- Operational MCP (transport, tools, auth)
- Durable Assistant conversation/confirmation storage
- Real payment providers
- `GET` current-user profile
- Productive CARD `DECLINED` simulation
- Object storage of product images

---

## 18. Riesgos residuales

| Risk | Impact | Current mitigation / residual |
|---|---|---|
| Logical UUIDs without cross-schema FK | Orphan references if a module writes an invalid id | Consumer ports validate existence; no physical FK by design |
| RAG treated as price/stock | Wrong commercial answers | Tools must call Catalog/Shopping/Orders; tests cover inactive products |
| In-memory Assistant state | Conversations and confirmation tokens lost on restart | Documented limitation; not durable |
| Simulated CARD never declines | No runtime declined-charge path | `ProcessPaymentUseCase` always `APPROVED` for CARD |
| Local refund timestamp | No external money movement | `refundedAt` on `APPROVED` only |
| JWT without refresh/revocation | Stolen token valid until expiry (~15 min) | Short TTL; no refresh subsystem |
| MCP stub | No alternative driving adapter | Package is non-operational |

Resolved in the current code and not listed as open architecture defects: non-atomic stock, checkout without a local transaction, Assistant impersonation via client user id, Knowledge HTTP left unsecured, INACTIVE products entering the cart through Catalog query.

---

## 19. Decisiones vigentes

Compatible with the current code:

- REST base `/api/v1` as documented in §10
- Pagination `page=0`, `size=20`, max `100` on order listing
- Public inactive product = 404
- RFC 7807 / `application/problem+json` with optional `code`
- One `User` type, one role, no self-register ADMIN, ADMIN not a shopper
- Address deactivate, not physical delete; snapshot on order
- COP only; `Money` in platform
- Category 1→N Product; `image_url` string; no image-storage port
- Payment statuses `PENDING` / `APPROVED` / `DECLINED`; refund via `refundedAt`
- Simulator: CARD always `APPROVED`; COD `PENDING`
- Checkout one local TX via Infrastructure `TransactionTemplate`
- Explicit `expectedUnitPrice`; 409 on change
- Idempotency-Key: fingerprint + materialized result; 24h retention
- Adjacent-only order transitions; customer cancel `PENDING` + 15 minutes from `createdAt`
- JWT access 15 minutes; no refresh
- PostgreSQL schemas per module except Assistant; logical cross-module UUIDs
- Single Maven module
- Catalog search: PostgreSQL ILIKE on name/brand/barcode
- UTC clock
- Knowledge `vector(1536)`, `text-embedding-3-small`, HNSW cosine
- Assistant `OpenAiChatAdapter` behind `LLMPort`
- MCP not operational
- `anyRequest().denyAll()`

This document follows the code. It does not claim alignment with other policy files if those files still describe `REFUNDED` as a payment status, a `presentation/rest` package, or deferred Knowledge HTTP.

---

## 20. Veredicto del documento

This blueprint is an as-built description of the implemented SuperFercho backend.

Where this text and the source disagree, the source and Flyway migrations win.

Residual items in §17 are absences in the current system, not approved next phases.
