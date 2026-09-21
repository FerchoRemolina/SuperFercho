import { request } from "@/shared/api/client";
import type { Money } from "@/shared/money/money";

/** Mirrors PaymentMethod used by POST /api/v1/orders. */
export type PaymentMethod = "SIMULATED_CARD" | "CASH_ON_DELIVERY";

/** Mirrors OrderStatus. A successful checkout always creates PENDING. */
export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PREPARING"
  | "READY"
  | "DELIVERED"
  | "CANCELLED";

/** Mirrors PaymentStatus. */
export type PaymentStatus = "PENDING" | "APPROVED" | "DECLINED";

/** Mirrors CheckoutItemRequest. */
export type CheckoutItemRequest = {
  productId: string;
  quantity: number;
  expectedUnitPrice: Money;
};

/** Mirrors CheckoutRequest. Identity comes from the JWT. */
export type CheckoutRequest = {
  addressId: string;
  paymentMethod: PaymentMethod;
  items: CheckoutItemRequest[];
};

/** Mirrors CheckoutRestResponse. Does not include items or shipping. */
export type CheckoutResponse = {
  orderId: string;
  orderNumber: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  total: Money;
};

/** Mirrors OrderItemRestResponse. */
export type OrderItem = {
  id: string;
  productId: string;
  productName: string;
  unitPrice: Money;
  quantity: number;
  subtotal: Money;
};

/** Mirrors ShippingAddressRestResponse. */
export type ShippingAddress = {
  recipientName: string;
  addressLine: string;
  additionalInfo: string | null;
  city: string;
  department: string;
  phone: string;
};

/** Mirrors OrderPaymentRestResponse. */
export type OrderPayment = {
  paymentId: string;
  amount: Money;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  providerReference: string | null;
  refundedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

/** Mirrors OrderRestResponse for GET /api/v1/orders and GET /api/v1/orders/{orderId}. */
export type Order = {
  id: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  items: OrderItem[];
  subtotal: Money;
  total: Money;
  shippingAddress: ShippingAddress;
  paymentId: string | null;
  createdAt: string;
  confirmedAt: string | null;
  cancelledAt: string | null;
  updatedAt: string;
  payment: OrderPayment | null;
};

/** Mirrors PagedOrdersRestResponse. Defaults: page=0, size=20, max size=100. */
export type PagedOrders = {
  items: Order[];
  page: number;
  size: number;
  totalElements: number;
};

export type ListOrdersParams = {
  page?: number;
  size?: number;
};

export function orderKeys() {
  return {
    all: ["orders"] as const,
    detail: (orderId: string) => ["orders", orderId] as const,
  };
}

export async function checkout(
  body: CheckoutRequest,
  idempotencyKey: string,
): Promise<CheckoutResponse> {
  return request<CheckoutResponse>("/orders", {
    method: "POST",
    body,
    idempotencyKey,
  });
}

export async function getOrder(orderId: string): Promise<Order> {
  return request<Order>(`/orders/${encodeURIComponent(orderId)}`);
}

export async function listOrders(
  params: ListOrdersParams = {},
): Promise<PagedOrders> {
  const search = new URLSearchParams();
  if (params.page !== undefined) {
    search.set("page", String(params.page));
  }
  if (params.size !== undefined) {
    search.set("size", String(params.size));
  }
  const query = search.toString();
  return request<PagedOrders>(query ? `/orders?${query}` : "/orders");
}

/** POST /api/v1/orders/{orderId}/cancel. No body; identity comes from the JWT. */
export async function cancelOrder(orderId: string): Promise<Order> {
  return request<Order>(`/orders/${encodeURIComponent(orderId)}/cancel`, {
    method: "POST",
  });
}

export const PAYMENT_METHODS: readonly PaymentMethod[] = [
  "SIMULATED_CARD",
  "CASH_ON_DELIVERY",
];

export function paymentMethodLabel(method: PaymentMethod): string {
  switch (method) {
    case "SIMULATED_CARD":
      return "Tarjeta simulada";
    case "CASH_ON_DELIVERY":
      return "Pago contra entrega";
  }
}

export function orderStatusLabel(status: OrderStatus): string {
  switch (status) {
    case "PENDING":
      return "Pendiente";
    case "CONFIRMED":
      return "Confirmado";
    case "PREPARING":
      return "En preparación";
    case "READY":
      return "Listo";
    case "DELIVERED":
      return "Entregado";
    case "CANCELLED":
      return "Cancelado";
  }
}

export function paymentStatusLabel(status: PaymentStatus): string {
  switch (status) {
    case "PENDING":
      return "Pendiente";
    case "APPROVED":
      return "Aprobado";
    case "DECLINED":
      return "Rechazado";
  }
}
