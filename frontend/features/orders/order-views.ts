import {
  orderKeys,
  paymentMethodLabel,
  paymentStatusLabel,
  type Order,
  type OrderPayment,
  type OrderStatus,
  type PagedOrders,
} from "@/features/orders/api";
import { isApiError, type ApiProblem } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";

export type QueryLike<T> = {
  isPending: boolean;
  isError: boolean;
  data?: T;
  error?: unknown;
};

export type OrdersListView =
  | { kind: "loading" }
  | { kind: "empty" }
  | { kind: "error"; title: string; message: string }
  | { kind: "success"; orders: Order[] };

export type OrderDetailView =
  | { kind: "loading" }
  | { kind: "error"; title: string; message: string }
  | { kind: "success"; order: Order };

export type OrderStatusTone = "neutral" | "primary" | "accent" | "danger";

export function ordersListView(
  query: QueryLike<PagedOrders>,
): OrdersListView {
  if (query.isPending) {
    return { kind: "loading" };
  }
  if (query.isError) {
    return { kind: "error", ...ordersListErrorCopy(query.error) };
  }
  const orders = query.data?.items ?? [];
  if (orders.length === 0) {
    return { kind: "empty" };
  }
  return { kind: "success", orders };
}

export function orderDetailView(query: QueryLike<Order>): OrderDetailView {
  if (query.isPending) {
    return { kind: "loading" };
  }
  if (query.isError || !query.data) {
    return { kind: "error", ...orderDetailErrorCopy(query.error) };
  }
  return { kind: "success", order: query.data };
}

export function ordersListErrorCopy(error: unknown): {
  title: string;
  message: string;
} {
  if (!isApiError(error)) {
    return {
      title: "No se pudieron cargar los pedidos",
      message: "No se pudieron cargar los pedidos.",
    };
  }
  return {
    title: "No se pudieron cargar los pedidos",
    message: messageForApiProblem(error.problem),
  };
}

export function orderDetailErrorCopy(error: unknown): {
  title: string;
  message: string;
} {
  const problem = problemFromError(error);
  if (!problem) {
    return {
      title: "No se pudo cargar el pedido",
      message: "No se pudo cargar el pedido.",
    };
  }
  const message = messageForApiProblem(problem);
  if (problem.status === 401) {
    return { title: "Sesión no autenticada", message };
  }
  if (problem.status === 403) {
    return {
      title: "Acceso denegado",
      message: "No tienes permisos para consultar este pedido.",
    };
  }
  if (problem.status === 404) {
    return { title: "Pedido no encontrado", message };
  }
  if (problem.status === 500) {
    return { title: "Error inesperado", message };
  }
  return { title: "No se pudo cargar el pedido", message };
}

export function orderDetailHref(orderId: string): string {
  return `/orders/${orderId}`;
}

export function orderItemCount(order: Order): number {
  return order.items.length;
}

export function orderItemCountLabel(order: Order): string {
  const count = orderItemCount(order);
  return count === 1 ? "1 producto" : `${count} productos`;
}

export function listPaymentSummary(order: Order): string | null {
  if (!order.payment) {
    return null;
  }
  return `${paymentMethodLabel(order.payment.paymentMethod)} · ${paymentStatusLabel(order.payment.status)}`;
}

export function orderStatusTone(status: OrderStatus): OrderStatusTone {
  switch (status) {
    case "PENDING":
      return "accent";
    case "CANCELLED":
      return "danger";
    case "CONFIRMED":
    case "PREPARING":
    case "READY":
    case "DELIVERED":
      return "primary";
  }
}

export function orderStatusHint(status: OrderStatus): string | null {
  switch (status) {
    case "PENDING":
      return "Puedes cancelarlo durante los primeros 15 minutos después de realizarlo.";
    case "CANCELLED":
      return "Este pedido fue cancelado.";
    default:
      return null;
  }
}

export function formatOrderDate(iso: string): string {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Bogota",
  }).format(new Date(iso));
}

function problemFromError(error: unknown): ApiProblem | null {
  if (isApiError(error)) {
    return error.problem;
  }
  return null;
}

export type CancelPanelState =
  | "hidden"
  | "expired"
  | "idle"
  | "confirming"
  | "pending";

/** Matches backend Order.CUSTOMER_CANCELLATION_WINDOW (15 minutes). */
export const CUSTOMER_CANCELLATION_WINDOW_MS = 15 * 60 * 1000;

export const CANCEL_WINDOW_IDLE_COPY =
  "Puedes cancelar este pedido durante los primeros 15 minutos después de realizarlo.";

export const CANCEL_WINDOW_EXPIRED_COPY =
  "El plazo de 15 minutos para cancelar este pedido ya terminó.";

export const CANCEL_CONFIRMATION_TITLE = "¿Cancelar este pedido?";
export const CANCEL_CONFIRMATION_BODY =
  "El pedido se cancelará y no podrás deshacerlo desde esta pantalla. El supermercado dejará de gestionarlo.";

export function customerCancellationDeadline(createdAt: string): Date {
  return new Date(
    new Date(createdAt).getTime() + CUSTOMER_CANCELLATION_WINDOW_MS,
  );
}

export function isWithinCustomerCancellationWindow(
  order: Order,
  now: Date = new Date(),
): boolean {
  if (order.status !== "PENDING") {
    return false;
  }
  return now.getTime() <= customerCancellationDeadline(order.createdAt).getTime();
}

export function canShowCancelAction(
  order: Order,
  now: Date = new Date(),
): boolean {
  return isWithinCustomerCancellationWindow(order, now);
}

export function cancellationRemainingLabel(
  createdAt: string,
  now: Date = new Date(),
): string | null {
  const remainingMs =
    customerCancellationDeadline(createdAt).getTime() - now.getTime();
  if (remainingMs <= 0) {
    return null;
  }
  const minutes = Math.max(1, Math.ceil(remainingMs / 60_000));
  if (minutes === 1) {
    return "Te queda aproximadamente 1 minuto para cancelar.";
  }
  return `Te quedan aproximadamente ${minutes} minutos para cancelar.`;
}

export function cancelPanelState({
  order,
  confirming,
  isPending,
  now = new Date(),
}: {
  order: Order;
  confirming: boolean;
  isPending: boolean;
  now?: Date;
}): CancelPanelState {
  if (order.status !== "PENDING") {
    return "hidden";
  }
  if (!isWithinCustomerCancellationWindow(order, now)) {
    return "expired";
  }
  if (isPending) {
    return "pending";
  }
  if (confirming) {
    return "confirming";
  }
  return "idle";
}

export function isCancelSubmitLocked(isPending: boolean): boolean {
  return isPending;
}

export function cancelOrderCacheKeys(orderId: string) {
  const keys = orderKeys();
  return {
    list: keys.all,
    detail: keys.detail(orderId),
  };
}

export function cancelErrorCopy(error: unknown): {
  title: string;
  message: string;
} {
  const problem = problemFromError(error);
  if (!problem) {
    return {
      title: "No se pudo cancelar el pedido",
      message: "No se pudo cancelar el pedido.",
    };
  }
  const message = messageForApiProblem(problem);
  if (problem.status === 401) {
    return { title: "Sesión no autenticada", message };
  }
  if (problem.status === 403) {
    return {
      title: "Acceso denegado",
      message: "No tienes permisos para cancelar este pedido.",
    };
  }
  if (problem.status === 404) {
    return { title: "Pedido no encontrado", message };
  }
  if (problem.code === "CANCELLATION_NOT_ALLOWED") {
    return { title: "No se puede cancelar", message };
  }
  if (problem.status === 500) {
    return { title: "Error inesperado", message };
  }
  return { title: "No se pudo cancelar el pedido", message };
}

export function paymentRefundLabel(payment: OrderPayment | null): string | null {
  if (!payment?.refundedAt) {
    return null;
  }
  return `Reembolso registrado el ${formatOrderDate(payment.refundedAt)}`;
}
