import { describe, expect, it } from "vitest";
import { orderStatusLabel, type Order } from "@/features/orders/api";
import {
  CANCEL_CONFIRMATION_BODY,
  CANCEL_CONFIRMATION_TITLE,
  formatOrderDate,
  canShowCancelAction,
  cancelErrorCopy,
  cancelOrderCacheKeys,
  cancelPanelState,
  isCancelSubmitLocked,
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

const order: Order = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  orderNumber: "ORD-P-1001",
  customerId: "11111111-1111-1111-1111-111111111111",
  status: "PENDING",
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
  confirmedAt: null,
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
  it("uses Spanish labels and distinct treatment for PENDING and CANCELLED", () => {
    expect(orderStatusLabel("PENDING")).toBe("Pendiente");
    expect(orderStatusLabel("CONFIRMED")).toBe("Confirmado");
    expect(orderStatusLabel("PREPARING")).toBe("En preparación");
    expect(orderStatusLabel("READY")).toBe("Listo");
    expect(orderStatusLabel("DELIVERED")).toBe("Entregado");
    expect(orderStatusLabel("CANCELLED")).toBe("Cancelado");
    expect(orderStatusTone("PENDING")).toBe("accent");
    expect(orderStatusTone("CANCELLED")).toBe("danger");
    expect(orderStatusHint("PENDING")).toContain("confirma");
    expect(orderStatusHint("CANCELLED")).toContain("cancelado");
  });

  it("formats createdAt in es-CO without inventing a different instant", () => {
    expect(formatOrderDate("2026-03-01T15:00:00Z")).toMatch(/2026/);
  });
});

describe("order cancellation presentation", () => {
  it("shows the cancel action only while the order is PENDING", () => {
    expect(canShowCancelAction(order)).toBe(true);
    expect(canShowCancelAction({ ...order, status: "CONFIRMED" })).toBe(false);
    expect(canShowCancelAction({ ...order, status: "PREPARING" })).toBe(false);
    expect(canShowCancelAction({ ...order, status: "READY" })).toBe(false);
    expect(canShowCancelAction({ ...order, status: "DELIVERED" })).toBe(false);
    expect(canShowCancelAction({ ...order, status: "CANCELLED" })).toBe(false);
  });

  it("moves from idle to confirmation then locks submit while pending", () => {
    expect(
      cancelPanelState({ order, confirming: false, isPending: false }),
    ).toBe("idle");
    expect(
      cancelPanelState({ order, confirming: true, isPending: false }),
    ).toBe("confirming");
    expect(
      cancelPanelState({ order, confirming: true, isPending: true }),
    ).toBe("pending");
    expect(isCancelSubmitLocked(false)).toBe(false);
    expect(isCancelSubmitLocked(true)).toBe(true);
    expect(CANCEL_CONFIRMATION_TITLE).toMatch(/Cancelar/);
    expect(CANCEL_CONFIRMATION_BODY).toMatch(/no podrás deshacerlo/);
    expect(CANCEL_CONFIRMATION_BODY).not.toMatch(/reembolso/i);
  });

  it("hides the panel for a successful cancelled order", () => {
    expect(
      cancelPanelState({
        order: { ...order, status: "CANCELLED" },
        confirming: true,
        isPending: false,
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
      message: "Este pedido ya no puede cancelarse.",
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
