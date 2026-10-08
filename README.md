# SuperFercho

SuperFercho es una aplicación de comercio electrónico para supermercado, con tienda web, administración y un asistente llamado Fercho.

El proyecto está construido como un monolito modular con Spring Boot y una aplicación Next.js separada para el frontend.

## Stack

### Backend

- Java 21
- Spring Boot 3.5
- PostgreSQL
- Flyway
- pgvector
- Spring Security
- JWT
- Testcontainers

### Frontend

- Next.js 16
- React
- TypeScript
- App Router

### Fercho

Fercho es el asistente de compras de SuperFercho. Puede consultar el catálogo, trabajar con el carrito y listas de compras, consultar pedidos y preparar operaciones como checkout y cancelación.

Las operaciones sensibles requieren confirmación explícita antes de ejecutarse.

Fercho funciona actualmente para clientes autenticados.

Para uso local se puede ejecutar con Ollama y Qwen2.5 7B Instruct.

## Funcionalidades

### Tienda

- Registro e inicio de sesión
- Catálogo y búsqueda de productos
- Categorías
- Carrito de compras
- Checkout
- Pedidos
- Direcciones
- Favoritos
- Listas de compras
- Cuenta del cliente

### Administración

El Admin Hub incluye:

- Productos e inventario
- Pedidos
- Clientes
- Analítica
- Contenido de la tienda

### Fercho

El chat está disponible en `/assistant` para clientes autenticados.

Fercho puede:

- Buscar y consultar productos
- Consultar el carrito
- Agregar, modificar y eliminar productos del carrito
- Consultar y administrar listas de compras
- Consultar pedidos
- Consultar direcciones
- Preparar checkout
- Preparar cancelaciones
- Consultar la base de conocimiento

Checkout y cancelación requieren una confirmación adicional antes de ejecutar la operación.

## Arquitectura

El backend está organizado como un monolito modular.

```text
frontend/
    Next.js

backend/
    identity
    catalog
    shopping
    orders
    payments
    knowledge
    assistant
    storefront
    platform
    mcp
```

Los módulos mantienen sus límites mediante puertos, casos de uso y DTOs propios. El dominio no depende de Spring, JPA, HTTP ni del proveedor de LLM.

En Fercho, el flujo principal es:

```text
Assistant
    ↓
Tools
    ↓
Use Cases
    ↓
Domain
```

El asistente no accede directamente a la base de datos.

La identidad del cliente se obtiene del contexto autenticado y las operaciones sobre carrito, listas, pedidos y direcciones se realizan sobre los datos del usuario autenticado.

## Requisitos

Para ejecutar el proyecto localmente necesitas:

- JDK 21
- Node.js
- Docker
- Docker Compose
- Maven
- Ollama si quieres ejecutar Fercho localmente

## Configuración local

Clona el repositorio y crea tu archivo `.env` a partir de `.env.example`.

```powershell
Copy-Item .env.example .env
```

Inicia PostgreSQL:

```powershell
docker compose up -d
```

El backend usa PostgreSQL y Flyway para crear y actualizar el esquema.

### Iniciar el backend

Desde `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

### Iniciar el frontend

Desde `frontend/`:

```powershell
npm install
npm run dev
```

La aplicación Next.js quedará disponible en:

```text
http://localhost:3000
```

## Fercho local

La configuración local usa Ollama con Qwen2.5 7B Instruct.

En Windows:

```powershell
.\scripts\setup-fercho.ps1
```

En Linux/macOS:

```bash
./scripts/setup-fercho.sh
```

El script:

1. Comprueba que Ollama esté disponible.
2. Descarga `qwen2.5:7b-instruct` si es necesario.
3. Crea el modelo `fercho-qwen`.
4. Configura las variables necesarias en `.env`.
5. Ejecuta una prueba de tool calling.

Después de completar la configuración, inicia el backend normalmente.

### Usar otro modelo

Los scripts permiten indicar un modelo base diferente:

```powershell
.\scripts\setup-fercho.ps1 -Model <modelo>
```

La configuración resultante puede revisarse en `.env`.

## Fercho con un proveedor externo

También puedes utilizar un proveedor compatible con la API de OpenAI Chat Completions.

Configura en `.env`:

```text
OPENAI_API_KEY=<tu_api_key>
SUPERFERCHO_OPENAI_CHAT_URL=<chat_completions_url>
SUPERFERCHO_OPENAI_CHAT_MODEL=<modelo>
```

No se incluyen credenciales en el repositorio. Cada usuario debe proporcionar sus propias credenciales si utiliza un proveedor externo.

Para uso local con Ollama, el script de configuración genera la configuración correspondiente.

## Base de datos

Las migraciones se encuentran en:

```text
backend/src/main/resources/db/migration/
```

Flyway crea los esquemas y tablas necesarios al iniciar la aplicación.

El proyecto utiliza PostgreSQL y pgvector para la funcionalidad de conocimiento y búsqueda semántica.

## Tests

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Los tests de integración utilizan Testcontainers cuando requieren infraestructura externa.

Frontend:

```powershell
cd frontend
npm test
```

## Estructura del repositorio

```text
SuperFercho/
├── backend/                 # API y lógica de negocio
├── frontend/                # Aplicación web
├── docs/                    # Documentación técnica
├── scripts/                 # Scripts de desarrollo y configuración
├── docker-compose.yml
├── .env.example
└── README.md
```

Documentación adicional del frontend:

[`frontend/README.md`](frontend/README.md)

Arquitectura técnica del frontend:

[`docs/architecture/frontend-technical-blueprint.md`](docs/architecture/frontend-technical-blueprint.md)

## Seguridad

Las operaciones del backend requieren autenticación y autorización según el módulo y el rol.

Fercho está restringido actualmente a clientes autenticados.

Las operaciones sensibles del asistente no se ejecutan únicamente porque el modelo las solicite: el backend exige una confirmación explícita antes de realizar checkout o cancelar un pedido.

Las credenciales y secretos locales deben mantenerse en `.env`. Ese archivo no debe versionarse.

## Estado del proyecto

SuperFercho se encuentra en desarrollo.

La aplicación incluye actualmente la tienda, el Admin Hub y Fercho, además de los módulos de identidad, catálogo, compras, pedidos, pagos y conocimiento.

La documentación se mantiene junto al código y puede cambiar a medida que evoluciona el proyecto.
