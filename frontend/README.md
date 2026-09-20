# Frontend SuperFercho

Fundación técnica de la UI. Next.js App Router consume el REST de `backend/` y no duplica reglas de negocio.

Esta fase no implementa login, catálogo, carrito, checkout, pedidos, listas, admin, knowledge ni assistant. Las rutas existen como cascarones.

## Stack

- Next.js 16.3.5 (App Router)
- React 19.3.0
- TypeScript 5.9.3
- Tailwind CSS 4.3.3
- TanStack Query 5.103.1
- pnpm 12.4.2

## Requisitos

- Node.js 20.9 o superior (LTS 24 recomendado)
- pnpm 12.4.2 (`corepack pnpm` si pnpm no está en el PATH)

## Configuración

Copia `.env.example` a `.env.local` si necesitas cambiar el origen de Spring.

| Variable | Uso |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Base del navegador. Vacío = same-origin `/api/v1` |
| `SUPERFERCHO_API_ORIGIN` | Origen de Spring para el rewrite y fetch de servidor. Default `http://localhost:8080` |

No pongas `SUPERFERCHO_JWT_SECRET`, `OPENAI_API_KEY` ni passwords de base de datos en el frontend.

## Desarrollo local

En una terminal, el backend en `http://localhost:8080`. En otra:

```bash
cd frontend
corepack pnpm install
corepack pnpm dev
```

El navegador usa `http://localhost:3000`. Las llamadas a `/api/v1/*` se reescriben a Spring (`:8080`). No hace falta CORS en desarrollo.

## Scripts

```bash
corepack pnpm dev
corepack pnpm test
corepack pnpm lint
corepack pnpm typecheck
corepack pnpm build
```
