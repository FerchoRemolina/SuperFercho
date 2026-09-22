import type { ListAdminProductsQuery } from "@/features/admin/api";
import type { CategoryStatus, ProductStatus } from "@/features/catalog/api";
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

export const ADMIN_NAV_LINKS = [
  { href: "/admin", label: "Inicio", match: "exact" as const },
  { href: "/admin/products", label: "Productos", match: "prefix" as const },
  { href: "/admin/categories", label: "Categorías", match: "prefix" as const },
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
