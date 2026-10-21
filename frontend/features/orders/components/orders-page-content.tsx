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
import { PackageIcon } from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";

export function OrdersPageContent() {
  const ordersQuery = useOrdersQuery();
  const view = ordersListView(ordersQuery);

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <PageHeader
        title="Pedidos"
        description="Consulta el historial de tus pedidos y abre el detalle de cada uno."
      />

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
            align="center"
            className="py-14"
            icon={<PackageIcon className="size-6" />}
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ir al catálogo
              </Link>
            }
          />
        </div>
      ) : null}

      {view.kind === "success" ? (
        <ul className="mt-6 grid gap-3 md:mt-8 md:grid-cols-2 md:gap-4 xl:grid-cols-3">
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
    <div
      className="mt-6 grid gap-3 md:mt-8 md:grid-cols-2 md:gap-4 xl:grid-cols-3"
      aria-hidden="true"
    >
      {Array.from({ length: 3 }, (_, index) => (
        <Card key={index} className="grid gap-3 !p-4 md:!p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <Skeleton className="h-5 w-28" />
              <Skeleton className="mt-2 h-4 w-40" />
            </div>
            <Skeleton className="h-7 w-24 rounded-lg" />
          </div>
          <div className="flex items-end justify-between border-t border-sf-border pt-3">
            <div>
              <Skeleton className="h-3 w-10" />
              <Skeleton className="mt-2 h-7 w-24" />
            </div>
            <Skeleton className="h-4 w-28" />
          </div>
        </Card>
      ))}
    </div>
  );
}
