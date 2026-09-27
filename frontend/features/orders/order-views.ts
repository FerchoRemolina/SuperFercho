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
    case "CONFIRMED":
      return "accent";
    case "PREPARING":
    case "DELIVERY":
    case "DELIVERED":
      return "primary";
    case "CANCELLED":
      return "danger";
  }
}

/** Presentation-only accents on top of Badge tones (existing sf-* utilities). */
export function orderStatusBadgeClassName(
  status: OrderStatus,
): string | undefined {
  switch (status) {
    case "DELIVERY":
      return "ring-1 ring-inset ring-sf-primary/35";
    case "DELIVERED":
      return "!bg-sf-primary !text-white";
    default:
      return undefined;
  }
}

export function orderStatusHint(status: OrderStatus): string | null {
  switch (status) {
    case "CONFIRMED":
      return "Puedes cancelarlo durante los primeros 2 minutos después de confirmarlo.";
    case "PREPARING":
      return "El supermercado ya está preparando tu pedido.";
    case "DELIVERY":
      return "Tu pedido va en camino.";
    case "CANCELLED":
      return "Este pedido fue cancelado.";
    default:
      return null;
  }
}

/** Statuses the lifecycle job still advances; the UI polls while in one of them. */
export function isOrderInProgress(status: OrderStatus): boolean {
  return (
    status === "CONFIRMED" || status === "PREPARING" || status === "DELIVERY"
  );
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

export type CancelPanelState = "hidden" | "idle" | "confirming" | "pending";

/** Matches backend Order.CUSTOMER_CANCELLATION_WINDOW (2 minutes). */
export const CUSTOMER_CANCELLATION_WINDOW_MS = 2 * 60 * 1000;

/** Below this the countdown turns red: the window is about to close. */
export const CANCEL_COUNTDOWN_CRITICAL_MS = 60 * 1000;

/** Countdown tick while the order is still cancellable. */
export const CANCEL_COUNTDOWN_TICK_MS = 1000;

/** Poll interval while the order is still advancing through the lifecycle. */
export const ORDER_LIFECYCLE_POLL_MS = 10_000;

export const CANCEL_WINDOW_IDLE_COPY =
  "Puedes cancelar este pedido durante los primeros 2 minutos después de confirmarlo.";

export const CANCEL_CONFIRMATION_TITLE = "¿Cancelar este pedido?";
export const CANCEL_CONFIRMATION_BODY =
  "El pedido se cancelará y no podrás deshacerlo desde esta pantalla. El supermercado dejará de gestionarlo.";

/** Window is anchored on confirmedAt; createdAt is the fallback for older rows. */
export function cancellationAnchor(order: Order): string {
  return order.confirmedAt ?? order.createdAt;
}

export function customerCancellationDeadline(confirmedAt: string): Date {
  return new Date(
    new Date(confirmedAt).getTime() + CUSTOMER_CANCELLATION_WINDOW_MS,
  );
}

export function cancellationRemainingMs(
  order: Order,
  now: Date = new Date(),
): number {
  const remaining =
    customerCancellationDeadline(cancellationAnchor(order)).getTime() -
    now.getTime();
  return remaining > 0 ? remaining : 0;
}

/** MM:SS, rounded up so the last second is shown as 0:01 and never 0:00. */
export function formatCancellationCountdown(remainingMs: number): string {
  const totalSeconds = Math.max(0, Math.ceil(remainingMs / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

export function isCancellationCountdownCritical(remainingMs: number): boolean {
  return remainingMs > 0 && remainingMs <= CANCEL_COUNTDOWN_CRITICAL_MS;
}

export function isWithinCustomerCancellationWindow(
  order: Order,
  now: Date = new Date(),
): boolean {
  if (order.status !== "CONFIRMED") {
    return false;
  }
  return cancellationRemainingMs(order, now) > 0;
}

export function canShowCancelAction(
  order: Order,
  now: Date = new Date(),
): boolean {
  return isWithinCustomerCancellationWindow(order, now);
}

export function cancellationRemainingLabel(
  order: Order,
  now: Date = new Date(),
): string | null {
  const remainingMs = cancellationRemainingMs(order, now);
  if (remainingMs <= 0) {
    return null;
  }
  return `Te queda ${formatCancellationCountdown(remainingMs)} para cancelar.`;
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
  if (order.status !== "CONFIRMED") {
    return "hidden";
  }
  // Keep the panel mounted while the request is in flight, even if the
  // countdown reaches zero mid-submit.
  if (isPending) {
    return "pending";
  }
  if (!isWithinCustomerCancellationWindow(order, now)) {
    return "hidden";
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
