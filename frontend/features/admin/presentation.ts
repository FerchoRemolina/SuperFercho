import type {
  DocumentStatus,
  ListAdminOrdersQuery,
  ListAdminProductsQuery,
} from "@/features/admin/api";
import {
  ADMIN_DOCUMENT_STATUSES,
  ADMIN_ORDER_STATUSES,
  ADMIN_ORDERS_DEFAULT_PAGE,
  ADMIN_ORDERS_DEFAULT_SIZE,
} from "@/features/admin/api";
import type { CategoryStatus, ProductStatus } from "@/features/catalog/api";
import {
  orderStatusLabel,
  type OrderStatus,
  type PaymentStatus,
} from "@/features/orders/api";
import { isApiError } from "@/shared/errors/api-problem";
import type { Role } from "@/shared/session/session";

export function isAdminRole(role: Role | undefined): boolean {
  return role === "ADMIN";
}

export function shouldSearchAdminProducts(text: string): boolean {
  return text.trim().length > 0;
}

export function productStatusLabel(status: ProductStatus | CategoryStatus): string {
  return status === "ACTIVE" ? "Activo" : "Inactivo";
}

export function categoryStatusLabel(status: CategoryStatus): string {
  return productStatusLabel(status);
}

export function formatAdminInstant(iso: string): string {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Bogota",
  }).format(new Date(iso));
}

export function adminProductsHref(query: {
  text?: string;
  categoryId?: string;
  status?: ProductStatus | "";
}): string {
  const search = new URLSearchParams();
  const text = query.text?.trim() ?? "";
  if (text.length > 0) {
    search.set("text", text);
  }
  if (!shouldSearchAdminProducts(text)) {
    if (query.categoryId) {
      search.set("categoryId", query.categoryId);
    }
    if (query.status === "ACTIVE" || query.status === "INACTIVE") {
      search.set("status", query.status);
    }
  }
  const encoded = search.toString();
  return encoded ? `/admin/products?${encoded}` : "/admin/products";
}

export function listQueryFromSearchParams(params: {
  categoryId?: string;
  status?: string;
}): ListAdminProductsQuery {
  const categoryId = params.categoryId?.trim();
  const status = params.status === "ACTIVE" || params.status === "INACTIVE"
    ? params.status
    : undefined;
  return {
    categoryId: categoryId && categoryId.length > 0 ? categoryId : undefined,
    status,
  };
}

export function parseAdminOrderStatus(
  value: string | null | undefined,
): OrderStatus | undefined {
  const trimmed = value?.trim();
  if (!trimmed) {
    return undefined;
  }
  return (ADMIN_ORDER_STATUSES as readonly string[]).includes(trimmed)
    ? (trimmed as OrderStatus)
    : undefined;
}

export function parseAdminOrdersPage(value: string | null | undefined): number {
  if (value == null || value.trim() === "") {
    return ADMIN_ORDERS_DEFAULT_PAGE;
  }
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return ADMIN_ORDERS_DEFAULT_PAGE;
  }
  return parsed;
}

export function adminOrdersListQueryFromSearchParams(params: {
  page?: string | null;
  status?: string | null;
}): ListAdminOrdersQuery {
  return {
    page: parseAdminOrdersPage(params.page),
    size: ADMIN_ORDERS_DEFAULT_SIZE,
    status: parseAdminOrderStatus(params.status),
  };
}

export function adminOrdersHref(query: {
  page?: number;
  status?: OrderStatus | "";
}): string {
  const search = new URLSearchParams();
  const page = query.page ?? ADMIN_ORDERS_DEFAULT_PAGE;
  if (page > ADMIN_ORDERS_DEFAULT_PAGE) {
    search.set("page", String(page));
  }
  if (query.status && parseAdminOrderStatus(query.status)) {
    search.set("status", query.status);
  }
  const encoded = search.toString();
  return encoded ? `/admin/orders?${encoded}` : "/admin/orders";
}

export function adminOrdersPageCount(totalElements: number, size: number): number {
  if (size <= 0 || totalElements <= 0) {
    return 0;
  }
  return Math.ceil(totalElements / size);
}

export function canGoToPreviousAdminOrdersPage(page: number): boolean {
  return page > 0;
}

export function canGoToNextAdminOrdersPage(
  page: number,
  size: number,
  totalElements: number,
): boolean {
  if (size <= 0) {
    return false;
  }
  return (page + 1) * size < totalElements;
}

export function adminOrderDetailHref(orderId: string): string {
  return `/admin/orders/${encodeURIComponent(orderId)}`;
}

/** Next status for ADMIN POST /orders/{id}/status. Excludes CANCELLED. */
export function nextAdminOrderStatus(
  status: OrderStatus,
): OrderStatus | null {
  switch (status) {
    case "PENDING":
      return "CONFIRMED";
    case "CONFIRMED":
      return "PREPARING";
    case "PREPARING":
      return "READY";
    case "READY":
      return "DELIVERED";
    case "DELIVERED":
    case "CANCELLED":
      return null;
  }
}

export function adminOrderStatusAdvanceLabel(
  status: OrderStatus,
): string | null {
  switch (status) {
    case "PENDING":
      return "Confirmar pedido";
    case "CONFIRMED":
      return "Pasar a preparación";
    case "PREPARING":
      return "Marcar como listo";
    case "READY":
      return "Marcar como entregado";
    case "DELIVERED":
    case "CANCELLED":
      return null;
  }
}

export function canAdvanceAdminOrderStatus(status: OrderStatus): boolean {
  return nextAdminOrderStatus(status) !== null;
}

export type AdminOrderStatusPanelState =
  | "hidden"
  | "idle"
  | "confirming"
  | "pending";

export function adminOrderStatusPanelState(args: {
  status: OrderStatus;
  confirming: boolean;
  isPending: boolean;
}): AdminOrderStatusPanelState {
  if (!canAdvanceAdminOrderStatus(args.status)) {
    return "hidden";
  }
  if (args.isPending) {
    return "pending";
  }
  if (args.confirming) {
    return "confirming";
  }
  return "idle";
}

export function isAdminOrderStatusSubmitLocked(isPending: boolean): boolean {
  return isPending;
}

export function adminOrderStatusAdvanceConfirmation(args: {
  orderNumber: string;
  currentStatus: OrderStatus;
  nextStatus: OrderStatus;
}): { title: string; body: string } {
  return {
    title: "¿Cambiar el estado del pedido?",
    body: `El pedido ${args.orderNumber} pasará de ${orderStatusLabel(args.currentStatus)} a ${orderStatusLabel(args.nextStatus)}.`,
  };
}

export type AdminOrderDetailErrorKind =
  | "order_not_found"
  | "payment_not_found"
  | "error";

export function adminOrderDetailErrorKind(
  error: unknown,
): AdminOrderDetailErrorKind {
  if (!isApiError(error)) {
    return "error";
  }
  if (error.problem.code === "ORDER_NOT_FOUND") {
    return "order_not_found";
  }
  if (error.problem.code === "PAYMENT_NOT_FOUND") {
    return "payment_not_found";
  }
  return "error";
}

export function documentStatusLabel(status: DocumentStatus): string {
  switch (status) {
    case "RECEIVED":
      return "Recibido";
    case "CHUNKED":
      return "Fragmentado";
    case "READY":
      return "Listo";
    case "FAILED":
      return "Fallido";
    case "INACTIVE":
      return "Inactivo";
  }
}

export function canProcessKnowledgeDocument(status: DocumentStatus): boolean {
  return status === "RECEIVED" || status === "CHUNKED" || status === "FAILED";
}

export function canDeactivateKnowledgeDocument(status: DocumentStatus): boolean {
  return status === "READY";
}

export function canReactivateKnowledgeDocument(status: DocumentStatus): boolean {
  return status === "INACTIVE";
}

/** Backend replaceContent does not restrict by status; all statuses are allowed. */
export function canReplaceKnowledgeDocumentContent(
  status: DocumentStatus,
): boolean {
  return (ADMIN_DOCUMENT_STATUSES as readonly DocumentStatus[]).includes(status);
}

export function knowledgeDocumentProcessConfirmation(args: {
  title: string;
}): { title: string; body: string } {
  return {
    title: "¿Procesar este documento?",
    body: `Se fragmentará e indexará «${args.title}». La operación puede tardar porque se ejecuta de forma síncrona.`,
  };
}

export function knowledgeDocumentDeactivateConfirmation(args: {
  title: string;
}): { title: string; body: string } {
  return {
    title: "¿Desactivar este documento?",
    body: `«${args.title}» dejará de estar disponible para la búsqueda de conocimiento hasta que lo reactives.`,
  };
}

export function knowledgeDocumentReactivateConfirmation(args: {
  title: string;
}): { title: string; body: string } {
  return {
    title: "¿Reactivar este documento?",
    body: `«${args.title}» volverá al estado Listo y podrá usarse de nuevo en la búsqueda.`,
  };
}

export function knowledgeDocumentReplaceContentWarning(): string {
  return "Al reemplazar el contenido, el documento volverá a Recibido, se eliminarán sus fragmentos e índices, y deberás procesarlo de nuevo.";
}

export function adminKnowledgeHref(): string {
  return "/admin/knowledge";
}

export function adminKnowledgeNewHref(): string {
  return "/admin/knowledge/new";
}

export function adminKnowledgeDocumentHref(documentId: string): string {
  return `/admin/knowledge/${encodeURIComponent(documentId)}`;
}

export function adminPaymentHref(paymentId: string): string {
  return `/admin/payments/${encodeURIComponent(paymentId)}`;
}

export type AdminPaymentDetailErrorKind = "payment_not_found" | "error";

export function adminPaymentDetailErrorKind(
  error: unknown,
): AdminPaymentDetailErrorKind {
  if (isApiError(error) && error.problem.code === "PAYMENT_NOT_FOUND") {
    return "payment_not_found";
  }
  return "error";
}

export type PaymentStatusTone = "neutral" | "primary" | "accent" | "danger";

export function paymentStatusTone(status: PaymentStatus): PaymentStatusTone {
  switch (status) {
    case "APPROVED":
      return "primary";
    case "PENDING":
      return "accent";
    case "DECLINED":
      return "danger";
  }
}

export type DocumentStatusTone = "neutral" | "primary" | "accent" | "danger";

export function documentStatusTone(status: DocumentStatus): DocumentStatusTone {
  switch (status) {
    case "READY":
      return "primary";
    case "RECEIVED":
    case "CHUNKED":
      return "accent";
    case "FAILED":
      return "danger";
    case "INACTIVE":
      return "neutral";
  }
}

export const ADMIN_NAV_LINKS = [
  { href: "/admin", label: "Inicio", match: "exact" as const },
  { href: "/admin/products", label: "Productos", match: "prefix" as const },
  { href: "/admin/categories", label: "Categorías", match: "prefix" as const },
  { href: "/admin/orders", label: "Pedidos", match: "prefix" as const },
  { href: "/admin/knowledge", label: "Knowledge", match: "prefix" as const },
];

export function isAdminNavActive(
  href: string,
  pathname: string,
  match: "exact" | "prefix",
): boolean {
  if (match === "exact") {
    return pathname === href;
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}
