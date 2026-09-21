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
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Checkout
        </h1>
        <CheckoutSkeleton />
      </Container>
    );
  }

  if (cartQuery.isError) {
    const message = isApiError(cartQuery.error)
      ? messageForApiProblem(cartQuery.error.problem)
      : "No se pudo cargar el carrito.";
    return (
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Checkout
        </h1>
        <div className="mt-8 grid gap-4">
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
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Checkout
        </h1>
        <div className="mt-8">
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
      <Container as="main" className="py-10 md:py-16">
        <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
          Checkout
        </h1>
        <div className="mt-8 grid gap-4">
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
          "No pudimos obtener el precio vigente de uno o más productos. Revisa el pedido.",
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
    <Container as="main" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Checkout
      </h1>
      <p className="mt-2 max-w-2xl text-base text-sf-muted">
        El pedido se crea pendiente. El precio enviado es el vigente del
        catálogo, no el precio al agregar.
      </p>

      <div className="mt-8 grid gap-8 md:grid-cols-[minmax(0,1fr)_20rem] md:grid-rows-[auto_auto_1fr]">
        <section className="md:col-start-1 md:row-start-1">
          <Card className="grid gap-4">
            <h2 className="text-xl font-semibold text-sf-ink">Dirección</h2>
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
              <ul className="grid gap-3">
                {availableAddresses.map((address) => {
                  const checked = selectedAddress?.id === address.id;
                  return (
                    <li key={address.id}>
                      <label className="flex cursor-pointer gap-3 rounded-xl border border-sf-border p-4 has-[:checked]:border-sf-primary">
                        <input
                          type="radio"
                          name="checkout-address"
                          className="mt-1 size-4 accent-sf-primary"
                          checked={checked}
                          onChange={() => setSelectedAddressId(address.id)}
                        />
                        <span>
                          <span className="block font-semibold text-sf-ink">
                            {address.label}
                            {address.isDefault ? " · Predeterminada" : ""}
                          </span>
                          <span className="mt-1 block text-sm text-sf-muted">
                            {address.recipientName} · {address.addressLine},{" "}
                            {address.city}, {address.department}
                          </span>
                          <span className="mt-1 block text-sm text-sf-muted">
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
                className="text-sm font-semibold text-sf-primary hover:underline"
              >
                Gestionar direcciones
              </Link>
            ) : null}
          </Card>
        </section>

        <section className="md:col-start-1 md:row-start-3">
          <Card className="grid gap-4">
            <h2 className="text-xl font-semibold text-sf-ink">Productos</h2>
            <ul className="grid gap-4">
              {currentCart.items.map((item) => {
                const product = catalog.get(item.productId);
                const currentPrice = product?.price;
                return (
                  <li
                    key={item.id}
                    className="grid gap-3 sm:grid-cols-[4.5rem_minmax(0,1fr)] sm:items-center"
                  >
                    <div className="w-[4.5rem]">
                      <ProductImage src={product?.imageUrl ?? null} alt="" />
                    </div>
                    <div className="min-w-0">
                      <p className="font-semibold text-sf-ink">
                        {product?.name ?? "Producto no disponible"}
                      </p>
                      <p className="mt-1 text-sm text-sf-muted">
                        Cantidad: {item.quantity}
                      </p>
                      {currentPrice ? (
                        <p className="mt-1 text-sm font-semibold text-sf-ink">
                          Precio vigente: {formatMoney(currentPrice)}
                        </p>
                      ) : (
                        <p className="mt-1 text-sm text-sf-error">
                          No pudimos obtener el precio vigente.
                        </p>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
            <Link
              href="/cart"
              className="text-sm font-semibold text-sf-primary hover:underline"
            >
              Revisar carrito
            </Link>
          </Card>
        </section>

        <section className="md:col-start-1 md:row-start-2">
          <Card className="grid gap-4">
            <h2 className="text-xl font-semibold text-sf-ink">Método de pago</h2>
            <ul className="grid gap-3">
              {PAYMENT_METHODS.map((method) => (
                <li key={method}>
                  <label className="flex cursor-pointer gap-3 rounded-xl border border-sf-border p-4 has-[:checked]:border-sf-primary">
                    <input
                      type="radio"
                      name="checkout-payment"
                      className="mt-1 size-4 accent-sf-primary"
                      checked={paymentMethod === method}
                      onChange={() => setPaymentMethod(method)}
                    />
                    <span>
                      <span className="block font-semibold text-sf-ink">
                        {paymentMethodLabel(method)}
                      </span>
                      <span className="mt-1 block text-sm text-sf-muted">
                        {method === "SIMULATED_CARD"
                          ? "Simulación del MVP. No se piden datos de tarjeta."
                          : "El pago queda pendiente hasta la entrega."}
                      </span>
                    </span>
                  </label>
                </li>
              ))}
            </ul>
          </Card>
        </section>

        <aside className="sticky bottom-0 z-20 -mx-4 border-t border-sf-border bg-sf-surface px-4 py-4 md:col-start-2 md:row-start-1 md:row-span-3 md:mx-0 md:border-0 md:bg-transparent md:px-0 md:py-0 md:sticky md:top-24 md:bottom-auto">
          <Card className="grid gap-4">
            <h2 className="text-xl font-semibold text-sf-ink">Resumen</h2>
            <dl className="grid gap-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-sf-muted">Productos</dt>
                <dd className="font-semibold text-sf-ink">
                  {currentCart.items.length}
                </dd>
              </div>
              {previewTotal ? (
                <div className="flex justify-between gap-4 border-t border-sf-border pt-3 text-base">
                  <dt className="font-semibold text-sf-ink">
                    Total según precio vigente
                  </dt>
                  <dd className="font-bold text-sf-ink">
                    {formatMoney(previewTotal)}
                  </dd>
                </div>
              ) : null}
            </dl>
            <p className="text-sm text-sf-muted">
              El total final lo confirma el pedido. No uses el precio al
              agregar.
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
      className="mt-8 grid gap-8 md:grid-cols-[minmax(0,1fr)_20rem]"
      aria-hidden="true"
    >
      <div className="grid gap-4">
        <Card>
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="mt-4 h-20 w-full" />
        </Card>
        <Card>
          <Skeleton className="h-6 w-1/4" />
          <Skeleton className="mt-4 h-16 w-full" />
        </Card>
      </div>
      <Card>
        <Skeleton className="h-6 w-1/2" />
        <Skeleton className="mt-4 h-4 w-full" />
        <Skeleton className="mt-6 h-11 w-full" />
      </Card>
    </div>
  );
}
