"use client";

import Link from "next/link";
import { CartLine } from "@/features/cart/components/cart-line";
import { CartSummary } from "@/features/cart/components/cart-summary";
import { useCartQuery } from "@/features/cart/hooks";
import { useProductsQuery } from "@/features/catalog/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { CartIcon } from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";

export function CartPageContent() {
  const cartQuery = useCartQuery();
  const productsQuery = useProductsQuery();

  if (cartQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader title="Carrito" />
        <CartSkeleton />
      </Container>
    );
  }

  if (cartQuery.isError) {
    const message = isApiError(cartQuery.error)
      ? messageForApiProblem(cartQuery.error.problem)
      : "No se pudo cargar el carrito.";
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader title="Carrito" />
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title="No se pudo cargar el carrito">
            {message}
          </Alert>
          <Button type="button" variant="secondary" onClick={() => cartQuery.refetch()}>
            Reintentar
          </Button>
        </div>
      </Container>
    );
  }

  const cart = cartQuery.data;
  if (!cart || cart.items.length === 0) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader title="Carrito" />
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Tu carrito está vacío"
            description="Añade productos del catálogo para armar tu mercado."
            align="center"
            className="py-14"
            icon={<CartIcon className="size-6" />}
            action={
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ir al catálogo
              </Link>
            }
          />
        </div>
      </Container>
    );
  }

  const productsById = new Map(
    (productsQuery.data ?? []).map((product) => [product.id, product]),
  );
  const mutating = cartQuery.isFetching && !cartQuery.isPending;

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <PageHeader
        title="Carrito"
        meta={
          <p className="text-sm text-sf-muted">
            {cart.items.length === 1
              ? "1 producto"
              : `${cart.items.length} productos`}
          </p>
        }
      />
      <div className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:items-start md:gap-6 lg:gap-8">
        <ul className="grid gap-2.5 md:gap-3">
          {cart.items.map((item) => (
            <CartLine
              key={item.id}
              item={item}
              product={productsById.get(item.productId)}
              busy={mutating}
            />
          ))}
        </ul>
        <aside className="md:sticky md:top-24">
          <CartSummary cart={cart} />
        </aside>
      </div>
    </Container>
  );
}

function CartSkeleton() {
  return (
    <div
      className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:gap-6 lg:gap-8"
      aria-hidden="true"
    >
      <div className="grid gap-2.5 md:gap-3">
        <Card className="grid grid-cols-[4.5rem_1fr] gap-3 !p-3">
          <Skeleton className="aspect-square w-full rounded-xl" />
          <div>
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="mt-2 h-4 w-1/3" />
            <Skeleton className="mt-4 h-11 w-36" />
          </div>
        </Card>
        <Card className="grid grid-cols-[4.5rem_1fr] gap-3 !p-3">
          <Skeleton className="aspect-square w-full rounded-xl" />
          <div>
            <Skeleton className="h-5 w-1/2" />
            <Skeleton className="mt-2 h-4 w-1/4" />
            <Skeleton className="mt-4 h-11 w-36" />
          </div>
        </Card>
      </div>
      <Card className="grid gap-3 !p-4 md:!p-5">
        <Skeleton className="h-6 w-1/2" />
        <Skeleton className="mt-2 h-4 w-full" />
        <Skeleton className="mt-2 h-4 w-2/3" />
        <Skeleton className="mt-4 h-11 w-full" />
      </Card>
    </div>
  );
}
