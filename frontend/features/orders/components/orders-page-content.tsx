"use client";

import Link from "next/link";
import { OrderCard } from "@/features/orders/components/order-card";
import { useOrdersQuery } from "@/features/orders/hooks";
import { ordersListView } from "@/features/orders/order-views";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function OrdersPageContent() {
  const ordersQuery = useOrdersQuery();
  const view = ordersListView(ordersQuery);

  return (
    <Container as="main" className="py-8 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Pedidos
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Consulta el historial de tus pedidos y abre el detalle de cada uno.
      </p>

      {view.kind === "loading" ? <OrdersSkeleton /> : null}

      {view.kind === "error" ? (
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title={view.title}>
            {view.message}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => ordersQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      ) : null}

      {view.kind === "empty" ? (
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Aún no tienes pedidos"
            description="Cuando confirmes un pedido desde el checkout, aparecerá aquí."
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ir al catálogo
              </Link>
            }
          />
        </div>
      ) : null}

      {view.kind === "success" ? (
        <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4">
          {view.orders.map((order) => (
            <li key={order.id}>
              <OrderCard order={order} />
            </li>
          ))}
        </ul>
      ) : null}
    </Container>
  );
}

function OrdersSkeleton() {
  return (
    <div className="mt-6 grid gap-3 md:mt-8 md:gap-4" aria-hidden="true">
      {Array.from({ length: 2 }, (_, index) => (
        <Card key={index} className="grid gap-2.5 !p-4">
          <div className="flex items-start justify-between gap-3">
            <Skeleton className="h-5 w-28" />
            <Skeleton className="h-7 w-24 rounded-lg" />
          </div>
          <Skeleton className="h-7 w-32" />
          <Skeleton className="h-4 w-40" />
          <Skeleton className="h-3 w-48" />
        </Card>
      ))}
    </div>
  );
}
