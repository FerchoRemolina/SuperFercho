# SuperFercho

SuperFercho es un MVP de supermercado inteligente. El backend se construye como un **monolito modular** con principios de **Clean Architecture / Arquitectura Hexagonal**.

Los canales previstos (REST y asistente de IA) usarán los mismos casos de uso de aplicación. Esos canales **aún no están implementados**.

## Fase actual

**Fase 0 — Fundación técnica.**

Esta fase solo establece el proyecto Maven/Spring Boot, la configuración, PostgreSQL + pgvector para desarrollo local y la estructura de paquetes. **Los módulos de negocio se implementan en fases posteriores.**

No existe todavía:

- autenticación / JWT
- catálogo, carrito, pedidos ni pagos
- RAG, LLM ni MCP

## Stack

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 3.x | Aplicación |
| Maven (wrapper) | Compilación |
| PostgreSQL + pgvector | Base de datos única (sin H2) |
| Flyway | Evolución de esquema |
| JUnit 5 | Pruebas |

## Arquitectura

Monolito modular de un solo artefacto Maven. Paquete raíz: `com.superfercho`.

| Módulo | Responsabilidad (fases posteriores) |
|---|---|
| `identity` | Usuarios, direcciones, autenticación |
| `catalog` | Productos, categorías, stock |
| `shopping` | Carrito y listas de compras |
| `orders` | Pedidos y checkout |
| `payments` | Pagos simulados |
| `knowledge` | RAG / documentos |
| `assistant` | Conversación y orquestación de IA |
| `mcp` | Capa de integración (no es un módulo de negocio) |
| `platform` | Conceptos transversales mínimos (`Money`, `Clock`, errores HTTP) |

La especificación de arquitectura está en [`docs/architecture/backend-technical-blueprint.md`](docs/architecture/backend-technical-blueprint.md).

## Requisitos

- JDK 21
- Docker y Docker Compose, para PostgreSQL local

No hace falta instalar Maven de forma global: el repositorio incluye Maven Wrapper.

## PostgreSQL local

```bat
docker compose up -d
```

La imagen es `pgvector/pgvector:pg16` (PostgreSQL con la extensión pgvector). Credenciales de desarrollo local: ver `.env.example`.

No copies secretos reales al repositorio. No commitees un archivo `.env`.

## Cómo ejecutar la aplicación

Con PostgreSQL en marcha:

```bat
mvnw.cmd spring-boot:run
```

En Unix/macOS/Linux:

```bash
./mvnw spring-boot:run
```

El perfil `local` puede activarse con `SPRING_PROFILES_ACTIVE=local`.

La aplicación escucha en el puerto `8080` por defecto (`SERVER_PORT`).

## Cómo ejecutar las pruebas

Windows:

```bat
mvnw.cmd test
```

Unix/macOS/Linux:

```bash
./mvnw test
```

Las pruebas de Fase 0 comprueban que el contexto de Spring arranca y que `Money` cumple las reglas de COP. **No usan H2.** Tampoco arrancan PostgreSQL: la conexión real a la base se verifica cuando se ejecuta la aplicación con Docker Compose.

## Configuración

Valores sensibles salen de variables de entorno. Plantilla: `.env.example`.

| Variable | Uso |
|---|---|
| `SUPERFERCHO_DB_URL` | JDBC de PostgreSQL |
| `SUPERFERCHO_DB_USERNAME` | Usuario de base de datos |
| `SUPERFERCHO_DB_PASSWORD` | Contraseña de base de datos (solo local en el ejemplo) |
| `SERVER_PORT` | Puerto HTTP |

Hibernate **no** crea ni actualiza el esquema (`ddl-auto=none`). Flyway es el dueño de las migraciones. En Fase 0 solo se habilita la extensión `vector`; no hay tablas de negocio.
