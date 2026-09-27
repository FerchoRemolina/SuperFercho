import { describe, expect, it } from "vitest";
import { orderStatusLabel, type Order } from "@/features/orders/api";
import {
  CANCEL_CONFIRMATION_BODY,
  CANCEL_CONFIRMATION_TITLE,
  CANCEL_COUNTDOWN_CRITICAL_MS,
  CANCEL_WINDOW_IDLE_COPY,
  CUSTOMER_CANCELLATION_WINDOW_MS,
  ORDER_LIFECYCLE_POLL_MS,
  formatOrderDate,
  canShowCancelAction,
  cancelErrorCopy,
  cancelOrderCacheKeys,
  cancellationRemainingLabel,
  cancellationRemainingMs,
  cancelPanelState,
  formatCancellationCountdown,
  isCancellationCountdownCritical,
  isCancelSubmitLocked,
  isOrderInProgress,
  isWithinCustomerCancellationWindow,
  listPaymentSummary,
  orderDetailErrorCopy,
  orderDetailHref,
  orderDetailView,
  orderItemCount,
  orderItemCountLabel,
  orderStatusHint,
  orderStatusTone,
  ordersListView,
  paymentRefundLabel,
} from "@/features/orders/order-views";
import { ApiError } from "@/shared/errors/api-problem";

const now = new Date("2026-03-01T15:00:30Z");

const order: Order = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  orderNumber: "ORD-P-1001",
  customerId: "11111111-1111-1111-1111-111111111111",
  status: "CONFIRMED",
  items: [
    {
      id: "99999999-9999-9999-9999-000000000001",
      productId: "33333333-3333-3333-3333-333333333333",
      productName: "Leche entera",
      unitPrice: { amount: 10.5, currency: "COP" },
      quantity: 2,
      subtotal: { amount: 21, currency: "COP" },
    },
  ],
  subtotal: { amount: 21, currency: "COP" },
  total: { amount: 21, currency: "COP" },
  shippingAddress: {
    recipientName: "Ada Lovelace",
    addressLine: "Calle 1 # 2-3",
    additionalInfo: "Apto 101",
    city: "Bogotá",
    department: "Cundinamarca",
    phone: "3001234567",
  },
  paymentId: "55555555-5555-5555-5555-555555555555",
  createdAt: "2026-03-01T15:00:00Z",
  confirmedAt: "2026-03-01T15:00:00Z",
  cancelledAt: null,
  updatedAt: "2026-03-01T15:00:00Z",
  payment: {
    paymentId: "55555555-5555-5555-5555-555555555555",
    amount: { amount: 21, currency: "COP" },
    paymentMethod: "SIMULATED_CARD",
    status: "APPROVED",
    providerReference: "sim-approved",
    refundedAt: null,
    createdAt: "2026-03-01T15:00:00Z",
    updatedAt: "2026-03-01T15:00:00Z",
  },
};

describe("orders list view", () => {
  it("is loading while the query is pending", () => {
    expect(
      ordersListView({ isPending: true, isError: false }),
    ).toEqual({ kind: "loading" });
  });

  it("is empty when the page has no orders", () => {
    expect(
      ordersListView({
        isPending: false,
        isError: false,
        data: { items: [], page: 0, size: 20, totalElements: 0 },
      }),
    ).toEqual({ kind: "empty" });
  });

  it("lists orders without inventing payment when the list omits it", () => {
    const listed = { ...order, payment: null };
    const view = ordersListView({
      isPending: false,
      isError: false,
      data: { items: [listed], page: 0, size: 20, totalElements: 1 },
    });
    expect(view).toEqual({ kind: "success", orders: [listed] });
    expect(listPaymentSummary(listed)).toBeNull();
    expect(orderItemCount(listed)).toBe(1);
    expect(orderItemCountLabel(listed)).toBe("1 producto");
    expect(orderDetailHref(listed.id)).toBe(
      "/orders/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    );
  });

  it("maps RFC7807 list errors without leaking English detail as the title", () => {
    const view = ordersListView({
      isPending: false,
      isError: true,
      error: new ApiError({
        status: 500,
        code: "INTERNAL_ERROR",
        title: "Internal Server Error",
        detail: "NullPointerException at OrderController",
      }),
    });
    expect(view.kind).toBe("error");
    if (view.kind !== "error") {
      throw new Error("expected error view");
    }
    expect(view.title).toBe("No se pudieron cargar los pedidos");
    expect(view.message).toBe("Ocurrió un error interno. Inténtalo más tarde.");
  });
});

describe("order detail view", () => {
  it("is loading while the query is pending", () => {
    expect(orderDetailView({ isPending: true, isError: false })).toEqual({
      kind: "loading",
    });
  });

  it("exposes persisted items, shipping snapshot and payment", () => {
    const view = orderDetailView({
      isPending: false,
      isError: false,
      data: order,
    });
    expect(view).toEqual({ kind: "success", order });
    if (view.kind !== "success") {
      throw new Error("expected success view");
    }
    expect(view.order.items[0]?.productName).toBe("Leche entera");
    expect(view.order.items[0]?.quantity).toBe(2);
    expect(view.order.items[0]?.unitPrice).toEqual({
      amount: 10.5,
      currency: "COP",
    });
    expect(view.order.shippingAddress).toEqual({
      recipientName: "Ada Lovelace",
      addressLine: "Calle 1 # 2-3",
      additionalInfo: "Apto 101",
      city: "Bogotá",
      department: "Cundinamarca",
      phone: "3001234567",
    });
    expect(view.order.payment?.paymentMethod).toBe("SIMULATED_CARD");
    expect(view.order.payment?.status).toBe("APPROVED");
    expect(listPaymentSummary(view.order)).toBe(
      "Tarjeta simulada · Aprobado",
    );
  });

  it("maps 404 ORDER_NOT_FOUND without mentioning other customers", () => {
    const view = orderDetailView({
      isPending: false,
      isError: true,
      error: new ApiError({
        status: 404,
        code: "ORDER_NOT_FOUND",
        detail: "Order not found: bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      }),
    });
    expect(view).toEqual({
      kind: "error",
      title: "Pedido no encontrado",
      message: "No encontramos ese pedido.",
    });
    expect(JSON.stringify(view)).not.toContain("customer");
  });

  it("maps 403 ACCESS_DENIED as acceso denegado", () => {
    expect(
      orderDetailErrorCopy(
        new ApiError({
          status: 403,
          code: "ACCESS_DENIED",
          detail: "Access is denied",
        }),
      ),
    ).toEqual({
      title: "Acceso denegado",
      message: "No tienes permisos para consultar este pedido.",
    });
  });

  it("maps 500 as an unexpected error without leaking the stack", () => {
    expect(
      orderDetailErrorCopy(
        new ApiError({
          status: 500,
          code: "INTERNAL_ERROR",
          title: "Internal Server Error",
          detail: "java.lang.IllegalStateException",
        }),
      ),
    ).toEqual({
      title: "Error inesperado",
      message: "Ocurrió un error interno. Inténtalo más tarde.",
    });
  });

  it("maps 401 as sesión no autenticada", () => {
    expect(
      orderDetailErrorCopy(
        new ApiError({
          status: 401,
          code: "UNAUTHENTICATED",
          detail: "Authentication is required",
        }),
      ),
    ).toEqual({
      title: "Sesión no autenticada",
      message: "Debes iniciar sesión para continuar.",
    });
  });
});

describe("order status presentation", () => {
  it("uses Spanish labels and distinct treatment for the lifecycle statuses", () => {
    expect(orderStatusLabel("CONFIRMED")).toBe("Confirmado");
    expect(orderStatusLabel("PREPARING")).toBe("En preparación");
    expect(orderStatusLabel("DELIVERY")).toBe("En camino");
    expect(orderStatusLabel("DELIVERED")).toBe("Entregado");
    expect(orderStatusLabel("CANCELLED")).toBe("Cancelado");
    expect(orderStatusTone("CONFIRMED")).toBe("accent");
    expect(orderStatusTone("PREPARING")).toBe("primary");
    expect(orderStatusTone("DELIVERY")).toBe("primary");
    expect(orderStatusTone("DELIVERED")).toBe("primary");
    expect(orderStatusTone("CANCELLED")).toBe("danger");
    expect(orderStatusHint("CONFIRMED")).toContain("2 minutos");
    expect(orderStatusHint("CANCELLED")).toContain("cancelado");
  });

  it("polls only while the lifecycle job can still advance the order", () => {
    expect(isOrderInProgress("CONFIRMED")).toBe(true);
    expect(isOrderInProgress("PREPARING")).toBe(true);
    expect(isOrderInProgress("DELIVERY")).toBe(true);
    expect(isOrderInProgress("DELIVERED")).toBe(false);
    expect(isOrderInProgress("CANCELLED")).toBe(false);
    expect(ORDER_LIFECYCLE_POLL_MS).toBe(10_000);
  });

  it("formats createdAt in es-CO without inventing a different instant", () => {
    expect(formatOrderDate("2026-03-01T15:00:00Z")).toMatch(/2026/);
  });
});

describe("order cancellation presentation", () => {
  it("shows the cancel action only while CONFIRMED and within 2 minutes of confirmedAt", () => {
    expect(canShowCancelAction(order, now)).toBe(true);
    expect(CUSTOMER_CANCELLATION_WINDOW_MS).toBe(2 * 60 * 1000);
    expect(isWithinCustomerCancellationWindow(order, now)).toBe(true);
    expect(
      isWithinCustomerCancellationWindow(
        order,
        new Date("2026-03-01T15:01:59.999Z"),
      ),
    ).toBe(true);
    expect(
      isWithinCustomerCancellationWindow(
        order,
        new Date("2026-03-01T15:02:00Z"),
      ),
    ).toBe(false);
    expect(canShowCancelAction({ ...order, status: "PREPARING" }, now)).toBe(
      false,
    );
    expect(canShowCancelAction({ ...order, status: "DELIVERY" }, now)).toBe(
      false,
    );
    expect(canShowCancelAction({ ...order, status: "DELIVERED" }, now)).toBe(
      false,
    );
    expect(canShowCancelAction({ ...order, status: "CANCELLED" }, now)).toBe(
      false,
    );
  });

  it("anchors the window on confirmedAt and falls back to createdAt", () => {
    const reconfirmed = { ...order, confirmedAt: "2026-03-01T15:02:00Z" };
    expect(
      isWithinCustomerCancellationWindow(
        reconfirmed,
        new Date("2026-03-01T15:03:00Z"),
      ),
    ).toBe(true);
    expect(
      cancellationRemainingMs({ ...order, confirmedAt: null }, now),
    ).toBe(90_000);
  });

  it("counts down in MM:SS and turns critical in the last minute", () => {
    expect(formatCancellationCountdown(120_000)).toBe("2:00");
    expect(formatCancellationCountdown(90_000)).toBe("1:30");
    expect(formatCancellationCountdown(59_000)).toBe("0:59");
    expect(formatCancellationCountdown(1)).toBe("0:01");
    expect(formatCancellationCountdown(0)).toBe("0:00");
    expect(CANCEL_COUNTDOWN_CRITICAL_MS).toBe(60_000);
    expect(isCancellationCountdownCritical(61_000)).toBe(false);
    expect(isCancellationCountdownCritical(60_000)).toBe(true);
    expect(isCancellationCountdownCritical(1)).toBe(true);
    expect(isCancellationCountdownCritical(0)).toBe(false);
  });

  it("moves from idle to confirmation then locks submit while pending", () => {
    expect(
      cancelPanelState({ order, confirming: false, isPending: false, now }),
    ).toBe("idle");
    expect(
      cancelPanelState({ order, confirming: true, isPending: false, now }),
    ).toBe("confirming");
    expect(
      cancelPanelState({ order, confirming: true, isPending: true, now }),
    ).toBe("pending");
    expect(isCancelSubmitLocked(false)).toBe(false);
    expect(isCancelSubmitLocked(true)).toBe(true);
    expect(CANCEL_WINDOW_IDLE_COPY).toContain("2 minutos");
    expect(cancellationRemainingLabel(order, now)).toBe(
      "Te queda 1:30 para cancelar.",
    );
    expect(CANCEL_CONFIRMATION_TITLE).toMatch(/Cancelar/);
    expect(CANCEL_CONFIRMATION_BODY).toMatch(/no podrás deshacerlo/);
    expect(CANCEL_CONFIRMATION_BODY).not.toMatch(/reembolso/i);
  });

  it("hides the panel once the countdown reaches zero", () => {
    const expiredNow = new Date("2026-03-01T15:02:00Z");
    expect(
      cancelPanelState({
        order,
        confirming: false,
        isPending: false,
        now: expiredNow,
      }),
    ).toBe("hidden");
    expect(cancellationRemainingMs(order, expiredNow)).toBe(0);
    expect(cancellationRemainingLabel(order, expiredNow)).toBeNull();
  });

  it("keeps the panel mounted while a cancel request is in flight", () => {
    expect(
      cancelPanelState({
        order,
        confirming: true,
        isPending: true,
        now: new Date("2026-03-01T15:02:00Z"),
      }),
    ).toBe("pending");
  });

  it("hides the panel for a successful cancelled order", () => {
    expect(
      cancelPanelState({
        order: { ...order, status: "CANCELLED" },
        confirming: true,
        isPending: false,
        now,
      }),
    ).toBe("hidden");
  });

  it("maps CANCELLATION_NOT_ALLOWED without claiming a refund", () => {
    expect(
      cancelErrorCopy(
        new ApiError({
          status: 409,
          code: "CANCELLATION_NOT_ALLOWED",
          detail: "customer cancellation window has expired",
        }),
      ),
    ).toEqual({
      title: "No se puede cancelar",
      message:
        "Este pedido ya no puede cancelarse. El plazo de 2 minutos terminó o el pedido ya cambió de estado.",
    });
  });

  it("maps cancel 404, 403 and 401 without leaking other customers", () => {
    expect(
      cancelErrorCopy(
        new ApiError({
          status: 404,
          code: "ORDER_NOT_FOUND",
          detail: "Order not found: bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        }),
      ),
    ).toEqual({
      title: "Pedido no encontrado",
      message: "No encontramos ese pedido.",
    });
    expect(
      cancelErrorCopy(
        new ApiError({
          status: 403,
          code: "ACCESS_DENIED",
          detail: "Access is denied",
        }),
      ),
    ).toEqual({
      title: "Acceso denegado",
      message: "No tienes permisos para cancelar este pedido.",
    });
    expect(
      cancelErrorCopy(
        new ApiError({
          status: 401,
          code: "UNAUTHENTICATED",
          detail: "Authentication is required",
        }),
      ),
    ).toEqual({
      title: "Sesión no autenticada",
      message: "Debes iniciar sesión para continuar.",
    });
  });

  it("invalidates list and detail keys after a successful cancel", () => {
    expect(cancelOrderCacheKeys(order.id)).toEqual({
      list: ["orders"],
      detail: ["orders", order.id],
    });
  });

  it("shows a refund label only when refundedAt is present", () => {
    expect(paymentRefundLabel(order.payment)).toBeNull();
    expect(
      paymentRefundLabel({
        paymentId: "55555555-5555-5555-5555-555555555555",
        amount: { amount: 21, currency: "COP" },
        paymentMethod: "SIMULATED_CARD",
        status: "APPROVED",
        providerReference: "sim-approved",
        refundedAt: "2026-03-01T15:10:00Z",
        createdAt: "2026-03-01T15:00:00Z",
        updatedAt: "2026-03-01T15:10:00Z",
      }),
    ).toMatch(/Reembolso registrado/);
    expect(
      paymentRefundLabel({
        paymentId: "55555555-5555-5555-5555-555555555555",
        amount: { amount: 21, currency: "COP" },
        paymentMethod: "CASH_ON_DELIVERY",
        status: "PENDING",
        providerReference: null,
        refundedAt: null,
        createdAt: "2026-03-01T15:00:00Z",
        updatedAt: "2026-03-01T15:00:00Z",
      }),
    ).toBeNull();
  });
});
