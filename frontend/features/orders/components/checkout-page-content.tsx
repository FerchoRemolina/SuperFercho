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
import {
  CartIcon,
  CashIcon,
  CheckCircleIcon,
  CreditCardIcon,
  MapPinIcon,
} from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

function SectionTitle({
  step,
  title,
  icon: Icon,
}: {
  step: number;
  title: string;
  icon?: typeof MapPinIcon;
}) {
  return (
    <div className="flex items-center gap-2.5">
      <span
        aria-hidden="true"
        className="flex size-7 shrink-0 items-center justify-center rounded-full bg-sf-primary-soft text-xs font-bold text-sf-primary"
      >
        {step}
      </span>
      <h2 className="text-base font-semibold text-sf-ink md:text-lg">{title}</h2>
      {Icon ? <Icon className="size-4 text-sf-muted" aria-hidden="true" /> : null}
    </div>
  );
}

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
  const [changingAddress, setChangingAddress] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod | null>(
    null,
  );
  const [localError, setLocalError] = useState<string | null>(null);

  if (cartQuery.isPending || addressesQuery.isPending) {
    return (
      <Container as="main" width="wide" className="pb-6 pt-8 md:pb-12 md:pt-12">
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
      <Container as="main" width="wide" className="pb-6 pt-8 md:pb-12 md:pt-12">
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
      <Container as="main" width="wide" className="pb-6 pt-8 md:pb-12 md:pt-12">
        <CheckoutHeading />
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="Tu carrito está vacío"
            description="Agrega productos del catálogo antes de realizar un pedido."
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

  if (addressesQuery.isError) {
    const message = isApiError(addressesQuery.error)
      ? messageForApiProblem(addressesQuery.error.problem)
      : "No se pudieron cargar las direcciones.";
    return (
      <Container as="main" width="wide" className="pb-6 pt-8 md:pb-12 md:pt-12">
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
    <Container as="main" width="wide" className="pb-6 pt-8 md:pb-12 md:pt-12">
      <CheckoutHeading />
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Revisa dirección, productos y pago antes de confirmar.
      </p>

      <div className="mt-6 grid gap-5 md:mt-8 md:grid-cols-[minmax(0,1fr)_20rem] md:grid-rows-[auto_auto_1fr] md:gap-6">
        <section className="md:col-start-1 md:row-start-1">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <SectionTitle step={1} title="Dirección" icon={MapPinIcon} />
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
            ) : selectedAddress ? (
              <>
                <div className="flex items-start gap-3 rounded-xl border border-sf-primary/30 bg-sf-primary/5 p-3.5">
                  <span
                    aria-hidden="true"
                    className="flex size-10 shrink-0 items-center justify-center rounded-full bg-sf-primary-soft"
                  >
                    <MapPinIcon className="size-5 text-sf-primary" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                      <p className="font-semibold text-sf-ink">
                        {selectedAddress.label}
                      </p>
                      {selectedAddress.isDefault ? (
                        <span className="rounded-full bg-sf-yellow-soft px-2 py-0.5 text-[11px] font-semibold text-sf-ink">
                          Predeterminada
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-1 text-sm font-medium text-sf-ink">
                      {selectedAddress.recipientName}
                    </p>
                    <p className="mt-0.5 text-sm text-sf-muted">
                      {selectedAddress.addressLine}
                    </p>
                    {selectedAddress.additionalInfo ? (
                      <p className="mt-0.5 text-sm text-sf-muted">
                        {selectedAddress.additionalInfo}
                      </p>
                    ) : null}
                    <p className="mt-0.5 text-sm text-sf-muted">
                      {selectedAddress.city}, {selectedAddress.department}
                    </p>
                    <p className="mt-0.5 text-sm tabular-nums text-sf-muted">
                      {selectedAddress.phone}
                    </p>
                  </div>
                </div>

                {changingAddress ? (
                  <ul className="grid gap-2">
                    {availableAddresses.map((address) => {
                      const checked = selectedAddress?.id === address.id;
                      return (
                        <li key={address.id}>
                          <label
                            className={cx(
                              "flex min-h-11 cursor-pointer gap-3 rounded-xl border p-3.5 transition-all duration-150",
                              checked
                                ? "border-sf-primary bg-sf-primary/5 shadow-[0_0_0_1px_var(--color-sf-primary)]"
                                : "border-sf-border bg-sf-surface hover:border-sf-primary/40",
                            )}
                          >
                            <input
                              type="radio"
                              name="checkout-address"
                              className="mt-1 size-4 shrink-0 accent-sf-primary"
                              checked={checked}
                              onChange={() => {
                                setSelectedAddressId(address.id);
                                setChangingAddress(false);
                              }}
                            />
                            <span className="min-w-0">
                              <span className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                                <span className="font-semibold text-sf-ink">
                                  {address.label}
                                </span>
                                {address.isDefault ? (
                                  <span className="rounded-full bg-sf-yellow-soft px-2 py-0.5 text-[11px] font-semibold text-sf-ink">
                                    Predeterminada
                                  </span>
                                ) : null}
                              </span>
                              <span className="mt-0.5 block text-sm text-sf-muted">
                                {address.recipientName} · {address.addressLine},{" "}
                                {address.city}, {address.department}
                              </span>
                              <span className="mt-0.5 block text-sm tabular-nums text-sf-muted">
                                {address.phone}
                              </span>
                            </span>
                          </label>
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  <button
                    type="button"
                    className="inline-flex min-h-11 items-center text-sm font-semibold text-sf-primary hover:underline"
                    onClick={() => setChangingAddress(true)}
                  >
                    Cambiar dirección
                  </button>
                )}
                <Link
                  href="/addresses"
                  className="inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:underline"
                >
                  Gestionar direcciones
                </Link>
              </>
            ) : null}
          </Card>
        </section>

        <section className="md:col-start-1 md:row-start-3">
          <Card className="grid gap-3 !p-4 md:gap-4 md:!p-5">
            <SectionTitle step={3} title="Productos" />
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
                    className="grid grid-cols-[4rem_minmax(0,1fr)] items-start gap-3 border-b border-sf-border pb-2.5 last:border-b-0 last:pb-0 sm:grid-cols-[4rem_minmax(0,1fr)_auto] sm:gap-3.5"
                  >
                    <div className="w-14 shrink-0 sm:w-16">
                      <ProductImage src={product?.imageUrl ?? null} alt="" />
                    </div>
                    <div className="min-w-0">
                      <p className="line-clamp-2 text-sm font-semibold leading-snug text-sf-ink md:text-[0.95rem]">
                        {product?.name ?? "Producto no disponible"}
                      </p>
                      {brand ? (
                        <p className="mt-0.5 truncate text-[11px] font-medium uppercase tracking-[0.06em] text-sf-muted">
                          {brand}
                        </p>
                      ) : null}
                      {currentPrice ? (
                        <p className="mt-1 text-xs text-sf-muted">
                          Cantidad: {item.quantity} · {formatMoney(currentPrice)} c/u
                        </p>
                      ) : (
                        <p className="mt-1 text-xs text-sf-error">
                          Precio no disponible
                        </p>
                      )}
                    </div>
                    {lineAmount ? (
                      <p className="col-span-2 text-right text-base font-bold tabular-nums text-sf-ink sm:col-span-1 sm:pt-0.5">
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
            <SectionTitle step={2} title="Método de pago" icon={CheckCircleIcon} />
            <ul className="grid gap-2">
              {PAYMENT_METHODS.map((method) => {
                const checked = paymentMethod === method;
                const MethodIcon =
                  method === "SIMULATED_CARD" ? CreditCardIcon : CashIcon;
                return (
                  <li key={method}>
                    <label
                      className={cx(
                        "flex cursor-pointer items-center gap-3 rounded-xl border p-3.5 transition-all duration-150",
                        checked
                          ? "border-sf-primary bg-sf-primary/5 shadow-[0_0_0_1px_var(--color-sf-primary)]"
                          : "border-sf-border bg-sf-surface hover:border-sf-primary/40",
                      )}
                    >
                      <input
                        type="radio"
                        name="checkout-payment"
                        className="size-4 shrink-0 accent-sf-primary"
                        checked={checked}
                        onChange={() => setPaymentMethod(method)}
                      />
                      <span
                        aria-hidden="true"
                        className={cx(
                          "flex size-10 shrink-0 items-center justify-center rounded-full",
                          checked
                            ? "bg-sf-primary-soft text-sf-primary"
                            : "bg-sf-bg text-sf-muted",
                        )}
                      >
                        <MethodIcon className="size-5" />
                      </span>
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
                      {checked ? (
                        <CheckCircleIcon
                          aria-hidden="true"
                          className="ml-auto size-5 shrink-0 text-sf-primary"
                        />
                      ) : null}
                    </label>
                  </li>
                );
              })}
            </ul>
          </Card>
        </section>

        <aside className="sticky bottom-0 z-20 -mx-4 border-t border-sf-border bg-sf-surface/95 px-4 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3 shadow-[0_-4px_16px_rgba(23,33,27,0.06)] backdrop-blur-sm md:sticky md:bottom-auto md:top-24 md:col-start-2 md:row-span-3 md:row-start-1 md:mx-0 md:border-0 md:bg-transparent md:px-0 md:py-0 md:shadow-none md:backdrop-blur-none">
          <Card className="grid gap-3 !p-4 md:gap-3.5 md:!p-5">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-base font-semibold text-sf-ink md:text-lg">
                Resumen
              </h2>
              <span className="inline-flex items-center rounded-full bg-sf-primary-soft px-2.5 py-0.5 text-xs font-semibold text-sf-primary">
                {currentCart.items.length === 1
                  ? "1 producto"
                  : `${currentCart.items.length} productos`}
              </span>
            </div>
            {previewTotal ? (
              <>
                <dl className="grid gap-1.5 text-sm">
                  <div className="flex justify-between gap-4">
                    <dt className="text-sf-muted">Subtotal</dt>
                    <dd className="font-semibold tabular-nums text-sf-ink">
                      {formatMoney(previewTotal)}
                    </dd>
                  </div>
                </dl>
                <div className="flex items-baseline justify-between gap-4 border-t border-sf-border pt-3">
                  <p className="text-base font-semibold text-sf-ink">Total</p>
                  <p className="text-2xl font-bold tabular-nums text-sf-primary">
                    {formatMoney(previewTotal)}
                  </p>
                </div>
              </>
            ) : null}
            <div className="grid gap-1.5 text-xs leading-relaxed text-sf-muted">
              <p>Los precios se actualizan al confirmar tu pedido.</p>
              <p>
                El costo del domicilio se acuerda directamente con el
                domiciliario y no está incluido en este total.
              </p>
            </div>
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
              className="min-h-12 w-full text-base"
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
        className={cx(
          "mb-3 inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
          "text-sm font-semibold text-sf-muted transition-colors duration-150",
          "hover:border-sf-primary/40 hover:text-sf-primary md:mb-4",
        )}
      >
        ← Carrito
      </Link>
      <PageHeader title="Confirmar pedido" />
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
