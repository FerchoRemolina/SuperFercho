import type {
  ListAdminOrdersQuery,
  ListAdminProductsQuery,
} from "@/features/admin/api";
import {
  ADMIN_ORDER_STATUSES,
  ADMIN_ORDERS_DEFAULT_PAGE,
  ADMIN_ORDERS_DEFAULT_SIZE,
} from "@/features/admin/api";
import type { CategoryStatus, ProductStatus } from "@/features/catalog/api";
import type { OrderStatus } from "@/features/orders/api";
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

export const ADMIN_NAV_LINKS = [
  { href: "/admin", label: "Inicio", match: "exact" as const },
  { href: "/admin/products", label: "Productos", match: "prefix" as const },
  { href: "/admin/categories", label: "Categorías", match: "prefix" as const },
  { href: "/admin/orders", label: "Pedidos", match: "prefix" as const },
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
