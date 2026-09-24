"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { activeAddresses } from "@/features/account/api";
import { useAddressesQuery } from "@/features/account/hooks";
import { useCartQuery } from "@/features/cart/hooks";
import { catalogKeys } from "@/features/catalog/api";
import { useProductsQuery } from "@/features/catalog/hooks";
import { ProductImage } from "@/features/catalog/components/product-image";
import {
  buildCheckoutItems,
  expectedCheckoutTotal,
  loadCatalogProductsForCart,
  productsById,
} from "@/features/orders/checkout-prices";
import {
  PAYMENT_METHODS,
  paymentMethodLabel,
  type PaymentMethod,
} from "@/features/orders/api";
import { useCheckoutMutation } from "@/features/orders/hooks";
import {
  checkoutFingerprint,
  keyForAttempt,
  type CheckoutAttempt,
} from "@/features/orders/idempotency";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

export function CheckoutPageContent() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const cartQuery = useCartQuery();
  const addressesQuery = useAddressesQuery();
  const productsQuery = useProductsQuery();
  const checkoutMutation = useCheckoutMutation();
  const attemptRef = useRef<CheckoutAttempt | null>(null);
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(
    null,
  );
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod | null>(
    null,
  );
  const [localError, setLocalError] = useState<string | null>(null);

  if (cartQuery.isPending || addressesQuery.isPending) {
    return (
      <Container as="main" className="py-8 md:py-16">
        <CheckoutHeading />
        <CheckoutSkeleton />
      </Container>
    );
  }

  if (cartQuery.isError) {
    const message = isApiError(cartQuery.error)
      ? messageForApiProblem(cartQuery.error.problem)
      : "No se pudo cargar el carrito.";
    return (
      <Container as="main" className="py-8 md:py-16">
        <CheckoutHeading />
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title="No se pudo cargar el carrito">
            {message}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => cartQuery.refetch()}
          >
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
        <CheckoutHeading />
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Tu carrito está vacío"
            description="Agrega productos del catálogo antes de realizar un pedido."
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

  if (addressesQuery.isError) {
    const message = isApiError(addressesQuery.error)
      ? messageForApiProblem(addressesQuery.error.problem)
      : "No se pudieron cargar las direcciones.";
    return (
      <Container as="main" className="py-8 md:py-16">
        <CheckoutHeading />
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title="No se pudieron cargar las direcciones">
            {message}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => addressesQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      </Container>
    );
  }

  const availableAddresses = activeAddresses(addressesQuery.data ?? []);
  const defaultAddress =
    availableAddresses.find((address) => address.isDefault) ??
    availableAddresses[0] ??
    null;
  const selectedAddress =
    availableAddresses.find((address) => address.id === selectedAddressId) ??
    defaultAddress;
  const currentCart = cart;
  const catalog = productsById(productsQuery.data ?? []);
  const previewItems = buildCheckoutItems(currentCart.items, catalog);
  const previewTotal = previewItems
    ? expectedCheckoutTotal(previewItems)
    : null;
  const checkoutError = checkoutMutation.isError
    ? isApiError(checkoutMutation.error)
      ? messageForApiProblem(checkoutMutation.error.problem)
      : "No se pudo realizar el pedido."
    : localError;
  const checkoutErrorCode = checkoutMutation.isError
    ? isApiError(checkoutMutation.error)
      ? checkoutMutation.error.problem.code
      : undefined
    : undefined;
  const canConfirm =
    selectedAddress !== null &&
    paymentMethod !== null &&
    currentCart.items.length > 0 &&
    !checkoutMutation.isPending;

  async function confirmOrder() {
    setLocalError(null);
    if (!selectedAddress || !paymentMethod) {
      setLocalError("Selecciona una dirección y un método de pago.");
      return;
    }
    if (currentCart.items.length === 0) {
      setLocalError("Tu carrito está vacío.");
      return;
    }

    try {
      const products = await loadCatalogProductsForCart(currentCart.items);
      const items = buildCheckoutItems(
        currentCart.items,
        productsById(products),
      );
      if (!items) {
        setLocalError(
          "No pudimos obtener el precio de uno o más productos. Revisa el pedido.",
        );
        void queryClient.invalidateQueries({ queryKey: catalogKeys().all });
        return;
      }

      const body = {
        addressId: selectedAddress.id,
        paymentMethod,
        items,
      };
      const fingerprint = checkoutFingerprint(body);
      const attempt = keyForAttempt(attemptRef.current, fingerprint);
      attemptRef.current = attempt;

      const result = await checkoutMutation.mutateAsync({
        body,
        idempotencyKey: attempt.key,
      });
      router.push(`/orders/${result.orderId}`);
    } catch (error) {
      if (isApiError(error) && error.problem.code === "IDEMPOTENCY_CONFLICT") {
        attemptRef.current = null;
      }
    }
  }

  return (
    <Container as="main" className="pb-28 pt-8 md:pb-16 md:pt-16">
      <CheckoutHeading />
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Revisa dirección, productos y pago antes de confirmar.
      </p>

      <div className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:grid-rows-[auto_auto_1fr] md:gap-6">
        <section className="md:col-start-1 md:row-start-1">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
              Dirección
            </h2>
            {availableAddresses.length === 0 ? (
              <EmptyState
                title="No tienes una dirección disponible para realizar el pedido."
                description="Agrega o activa una dirección para continuar."
                action={
                  <Link href="/addresses" className={buttonClassName("primary")}>
                    Ir a direcciones
                  </Link>
                }
              />
            ) : (
              <ul className="grid gap-2">
                {availableAddresses.map((address) => {
                  const checked = selectedAddress?.id === address.id;
                  return (
                    <li key={address.id}>
                      <label
                        className={cx(
                          "flex min-h-11 cursor-pointer gap-3 rounded-xl border p-3 transition-colors",
                          checked
                            ? "border-sf-primary bg-sf-primary/5"
                            : "border-sf-border bg-sf-surface",
                        )}
                      >
                        <input
                          type="radio"
                          name="checkout-address"
                          className="mt-1 size-4 shrink-0 accent-sf-primary"
                          checked={checked}
                          onChange={() => setSelectedAddressId(address.id)}
                        />
                        <span className="min-w-0">
                          <span className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                            <span className="font-semibold text-sf-ink">
                              {address.label}
                            </span>
                            {address.isDefault ? (
                              <span className="text-xs font-medium text-sf-muted">
                                Predeterminada
                              </span>
                            ) : null}
                          </span>
                          <span className="mt-0.5 block text-sm text-sf-muted">
                            {address.recipientName} · {address.addressLine},{" "}
                            {address.city}, {address.department}
                          </span>
                          <span className="mt-0.5 block text-sm text-sf-muted">
                            {address.phone}
                          </span>
                        </span>
                      </label>
                    </li>
                  );
                })}
              </ul>
            )}
            {availableAddresses.length > 0 ? (
              <Link
                href="/addresses"
                className="inline-flex min-h-11 items-center text-sm font-semibold text-sf-primary hover:underline"
              >
                Gestionar direcciones
              </Link>
            ) : null}
          </Card>
        </section>

        <section className="md:col-start-1 md:row-start-3">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
              Productos
            </h2>
            <ul className="grid gap-2.5">
              {currentCart.items.map((item) => {
                const product = catalog.get(item.productId);
                const currentPrice = product?.price;
                const lineAmount = currentPrice
                  ? {
                      amount: currentPrice.amount * item.quantity,
                      currency: currentPrice.currency,
                    }
                  : null;
                const brand = product?.brand?.trim() || null;
                return (
                  <li
                    key={item.id}
                    className="grid grid-cols-[3.5rem_minmax(0,1fr)] items-start gap-3 border-b border-sf-border pb-2.5 last:border-b-0 last:pb-0 sm:grid-cols-[3.75rem_minmax(0,1fr)_auto] sm:gap-3.5"
                  >
                    <div className="w-[3.5rem] shrink-0 sm:w-[3.75rem]">
                      <ProductImage src={product?.imageUrl ?? null} alt="" />
                    </div>
                    <div className="min-w-0">
                      <p className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink md:text-base">
                        {product?.name ?? "Producto no disponible"}
                      </p>
                      {brand ? (
                        <p className="mt-0.5 truncate text-xs text-sf-muted md:text-sm">
                          {brand}
                        </p>
                      ) : null}
                      <p className="mt-1 text-sm font-medium text-sf-ink">
                        Cantidad: {item.quantity}
                      </p>
                      {currentPrice ? (
                        <p className="mt-0.5 text-xs text-sf-muted">
                          {formatMoney(currentPrice)} c/u
                        </p>
                      ) : (
                        <p className="mt-0.5 text-xs text-sf-error">
                          Precio no disponible
                        </p>
                      )}
                    </div>
                    {lineAmount ? (
                      <p className="col-span-2 text-right text-sm font-bold text-sf-ink sm:col-span-1 sm:pt-0.5 sm:text-base">
                        {formatMoney(lineAmount)}
                      </p>
                    ) : null}
                  </li>
                );
              })}
            </ul>
            <Link
              href="/cart"
              className="inline-flex min-h-11 items-center text-sm font-semibold text-sf-primary hover:underline"
            >
              Revisar carrito
            </Link>
          </Card>
        </section>

        <section className="md:col-start-1 md:row-start-2">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
              Método de pago
            </h2>
            <ul className="grid gap-2">
              {PAYMENT_METHODS.map((method) => {
                const checked = paymentMethod === method;
                return (
                  <li key={method}>
                    <label
                      className={cx(
                        "flex min-h-11 cursor-pointer gap-3 rounded-xl border p-3 transition-colors",
                        checked
                          ? "border-sf-primary bg-sf-primary/5"
                          : "border-sf-border bg-sf-surface",
                      )}
                    >
                      <input
                        type="radio"
                        name="checkout-payment"
                        className="mt-1 size-4 shrink-0 accent-sf-primary"
                        checked={checked}
                        onChange={() => setPaymentMethod(method)}
                      />
                      <span className="min-w-0">
                        <span className="block font-semibold text-sf-ink">
                          {paymentMethodLabel(method)}
                        </span>
                        <span className="mt-0.5 block text-sm text-sf-muted">
                          {method === "SIMULATED_CARD"
                            ? "Simulación de pago. No se piden datos de tarjeta reales."
                            : "El pago queda pendiente hasta la entrega."}
                        </span>
                      </span>
                    </label>
                  </li>
                );
              })}
            </ul>
          </Card>
        </section>

        <aside className="sticky bottom-0 z-20 -mx-4 border-t border-sf-border bg-sf-surface/95 px-4 py-3 shadow-[0_-4px_16px_rgba(23,33,27,0.06)] backdrop-blur-sm md:col-start-2 md:row-span-3 md:row-start-1 md:mx-0 md:border-0 md:bg-transparent md:px-0 md:py-0 md:shadow-none md:backdrop-blur-none md:sticky md:top-24 md:bottom-auto">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
              Resumen
            </h2>
            <dl className="grid gap-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-sf-muted">Productos</dt>
                <dd className="font-semibold text-sf-ink">
                  {currentCart.items.length}
                </dd>
              </div>
              {previewTotal ? (
                <>
                  <div className="flex justify-between gap-4">
                    <dt className="text-sf-muted">Subtotal</dt>
                    <dd className="font-semibold text-sf-ink">
                      {formatMoney(previewTotal)}
                    </dd>
                  </div>
                  <div className="flex justify-between gap-4 border-t border-sf-border pt-3">
                    <dt className="text-base font-semibold text-sf-ink">
                      Total
                    </dt>
                    <dd className="text-lg font-bold text-sf-ink">
                      {formatMoney(previewTotal)}
                    </dd>
                  </div>
                </>
              ) : null}
            </dl>
            <p className="text-xs leading-relaxed text-sf-muted">
              Los precios se actualizan al confirmar tu pedido.
            </p>
            {checkoutError ? (
              <Alert
                tone="error"
                title={checkoutErrorTitle(checkoutErrorCode)}
              >
                {checkoutError}
              </Alert>
            ) : null}
            <Button
              type="button"
              className="w-full"
              disabled={!canConfirm}
              onClick={() => void confirmOrder()}
            >
              {checkoutMutation.isPending
                ? "Confirmando…"
                : "Confirmar pedido"}
            </Button>
          </Card>
        </aside>
      </div>
    </Container>
  );
}

function CheckoutHeading() {
  return (
    <>
      <Link
        href="/cart"
        className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
      >
        ← Carrito
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Confirmar pedido
      </h1>
    </>
  );
}

function checkoutErrorTitle(code: string | undefined): string {
  switch (code) {
    case "PRODUCT_PRICE_CHANGED":
      return "El precio cambió";
    case "PRODUCT_NOT_AVAILABLE":
      return "Producto no disponible";
    case "STOCK_UNAVAILABLE":
      return "No hay existencias suficientes";
    case "CART_EMPTY":
      return "El carrito está vacío";
    case "ADDRESS_NOT_AVAILABLE":
      return "Dirección no disponible";
    case "PAYMENT_DECLINED":
      return "El pago no fue aprobado";
    case "IDEMPOTENCY_CONFLICT":
      return "Intento no válido";
    case "ACCESS_DENIED":
      return "Acceso no permitido";
    default:
      return "No se pudo realizar el pedido";
  }
}

function CheckoutSkeleton() {
  return (
    <div
      className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:gap-6"
      aria-hidden="true"
    >
      <div className="grid gap-4">
        <Card className="!p-4 md:!p-5">
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="mt-3 h-16 w-full" />
        </Card>
        <Card className="!p-4 md:!p-5">
          <Skeleton className="h-6 w-1/4" />
          <Skeleton className="mt-3 h-14 w-full" />
        </Card>
      </div>
      <Card className="!p-4 md:!p-5">
        <Skeleton className="h-6 w-1/2" />
        <Skeleton className="mt-3 h-4 w-full" />
        <Skeleton className="mt-5 h-11 w-full" />
      </Card>
    </div>
  );
}
