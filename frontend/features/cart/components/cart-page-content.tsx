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
import { Skeleton } from "@/shared/ui/skeleton";

export function CartPageContent() {
  const cartQuery = useCartQuery();
  const productsQuery = useProductsQuery();

  if (cartQuery.isPending) {
    return (
      <Container as="main" className="py-8 md:py-16">
        <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Carrito
        </h1>
        <CartSkeleton />
      </Container>
    );
  }

  if (cartQuery.isError) {
    const message = isApiError(cartQuery.error)
      ? messageForApiProblem(cartQuery.error.problem)
      : "No se pudo cargar el carrito.";
    return (
      <Container as="main" className="py-8 md:py-16">
        <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Carrito
        </h1>
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
      <Container as="main" className="py-8 md:py-16">
        <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Carrito
        </h1>
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Tu carrito está vacío"
            description="Añade productos del catálogo para armar tu mercado."
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
    <Container as="main" className="py-8 md:py-16">
      <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Carrito
      </h1>
      <div className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:items-start md:gap-8">
        <aside className="order-1 md:order-2 md:sticky md:top-24">
          <CartSummary cart={cart} />
        </aside>
        <ul className="order-2 grid gap-3 md:order-1 md:gap-4">
          {cart.items.map((item) => (
            <CartLine
              key={item.id}
              item={item}
              product={productsById.get(item.productId)}
              busy={mutating}
            />
          ))}
        </ul>
      </div>
    </Container>
  );
}

function CartSkeleton() {
  return (
    <div
      className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:gap-8"
      aria-hidden="true"
    >
      <Card className="order-1 grid gap-3 md:order-2">
        <Skeleton className="h-6 w-1/2" />
        <Skeleton className="mt-2 h-4 w-full" />
        <Skeleton className="mt-2 h-4 w-2/3" />
        <Skeleton className="mt-4 h-11 w-full" />
      </Card>
      <div className="order-2 grid gap-3 md:order-1">
        <Card className="grid grid-cols-[4.5rem_1fr] gap-3 p-3">
          <Skeleton className="aspect-square w-full" />
          <div>
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="mt-2 h-4 w-1/3" />
            <Skeleton className="mt-4 h-11 w-36" />
          </div>
        </Card>
        <Card className="grid grid-cols-[4.5rem_1fr] gap-3 p-3">
          <Skeleton className="aspect-square w-full" />
          <div>
            <Skeleton className="h-5 w-1/2" />
            <Skeleton className="mt-2 h-4 w-1/4" />
            <Skeleton className="mt-4 h-11 w-36" />
          </div>
        </Card>
      </div>
    </div>
  );
}
