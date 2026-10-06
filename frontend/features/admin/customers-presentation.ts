import type {
  AdminCustomerRecordStatus,
  AdminCustomerRecordStatusFilter,
  AdminCustomerSortBy,
  AdminCustomerSortDir,
  ListAdminCustomersQuery,
} from "@/features/admin/api";
import type {
  OrderStatus,
  PaymentStatus,
} from "@/features/orders/api";

/** Tipos de documento aceptados por Identity (CustomerRegistrationRules). */
export const ADMIN_CUSTOMER_DOCUMENT_TYPES = [
  { value: "CC", label: "Cédula de ciudadanía (CC)" },
  { value: "CE", label: "Cédula de extranjería (CE)" },
] as const;

export const ADMIN_CUSTOMERS_DEFAULT_PAGE = 0;
export const ADMIN_CUSTOMERS_DEFAULT_SIZE = 20;
export const ADMIN_CUSTOMERS_DEFAULT_SORT_BY: AdminCustomerSortBy = "CREATED_AT";
export const ADMIN_CUSTOMERS_DEFAULT_SORT_DIR: AdminCustomerSortDir = "DESC";

/** Perspectivas rápidas sobre el mismo listado (no son rutas distintas). */
export type AdminCustomersPerspective =
  | "all"
  | "recent"
  | "activity"
  | "spend"
  | "no_purchases";

export const ADMIN_CUSTOMERS_PERSPECTIVES: readonly {
  id: AdminCustomersPerspective;
  label: string;
}[] = [
  { id: "all", label: "Todos" },
  { id: "recent", label: "Registrados recientemente" },
  { id: "activity", label: "Mayor actividad" },
  { id: "spend", label: "Mayor gasto" },
  { id: "no_purchases", label: "Sin compras" },
] as const;

/** Opciones de ordenamiento comprensibles → sortBy + sortDir del backend. */
export const ADMIN_CUSTOMERS_SORT_OPTIONS: readonly {
  value: string;
  label: string;
  sortBy: AdminCustomerSortBy;
  sortDir: AdminCustomerSortDir;
}[] = [
  {
    value: "CREATED_AT:DESC",
    label: "Más recientes",
    sortBy: "CREATED_AT",
    sortDir: "DESC",
  },
  {
    value: "NAME:ASC",
    label: "Nombre",
    sortBy: "NAME",
    sortDir: "ASC",
  },
  {
    value: "DOCUMENT:ASC",
    label: "Documento",
    sortBy: "DOCUMENT",
    sortDir: "ASC",
  },
  {
    value: "ORDERS:DESC",
    label: "Más pedidos",
    sortBy: "ORDERS",
    sortDir: "DESC",
  },
  {
    value: "TOTAL_SPENT:DESC",
    label: "Mayor gasto",
    sortBy: "TOTAL_SPENT",
    sortDir: "DESC",
  },
  {
    value: "LAST_ORDER_AT:DESC",
    label: "Última compra",
    sortBy: "LAST_ORDER_AT",
    sortDir: "DESC",
  },
] as const;

export const ADMIN_CUSTOMERS_STATUS_FILTERS: readonly {
  value: AdminCustomerRecordStatusFilter;
  label: string;
}[] = [
  { value: "ALL", label: "Todos" },
  { value: "ACTIVE", label: "Activos" },
  { value: "INACTIVE", label: "Inactivos" },
] as const;

export type AdminCustomerDocumentType = (typeof ADMIN_CUSTOMER_DOCUMENT_TYPES)[number]["value"];

const DOCUMENT_NUMBER_PATTERN = /^\d{1,10}$/;

export function isValidAdminCustomerDocumentNumber(value: string): boolean {
  return DOCUMENT_NUMBER_PATTERN.test(value.trim());
}

export function canSearchAdminCustomer(
  documentType: string,
  documentNumber: string,
): boolean {
  return (
    ADMIN_CUSTOMER_DOCUMENT_TYPES.some((t) => t.value === documentType) &&
    isValidAdminCustomerDocumentNumber(documentNumber)
  );
}

export function adminCustomerDocumentTypeLabel(value: string): string {
  return (
    ADMIN_CUSTOMER_DOCUMENT_TYPES.find((t) => t.value === value)?.label ?? value
  );
}

export type AdminCustomerAccountStatus = "ACTIVE" | "INACTIVE" | "DELETED";

/** El estado DELETED se deriva de deletedAt; el UserStatus solo cubre
 *  ACTIVE/INACTIVE. */
export function adminCustomerAccountStatus(account: {
  status: "ACTIVE" | "INACTIVE";
  deletedAt: string | null;
}): AdminCustomerAccountStatus {
  return account.deletedAt ? "DELETED" : account.status;
}

export function adminCustomerAccountStatusLabel(
  status: AdminCustomerAccountStatus,
): string {
  if (status === "ACTIVE") return "Activa";
  if (status === "INACTIVE") return "Inactiva";
  return "Eliminada";
}

/** Etiquetas del estado derivado del CustomerRecord (listado). */
export function adminCustomerRecordStatusLabel(
  status: AdminCustomerRecordStatus,
): string {
  if (status === "INACTIVE") return "Inactivo";
  return "Activo";
}

/** No depender solo del color: cada estado tiene su etiqueta textual. */
export function adminCustomerAccountToneClass(
  status: AdminCustomerAccountStatus,
): string {
  if (status === "ACTIVE") return "bg-emerald-50 text-emerald-700";
  if (status === "INACTIVE") return "bg-slate-100 text-slate-600";
  return "bg-slate-200 text-slate-500";
}

export function adminCustomerRecordStatusToneClass(
  status: AdminCustomerRecordStatus,
): string {
  if (status === "INACTIVE") return "bg-slate-100 text-slate-600";
  return "bg-emerald-50 text-emerald-700";
}

export function adminCustomerDisplayName(customer: {
  billingFirstName: string;
  billingLastName: string;
}): string {
  return `${customer.billingFirstName} ${customer.billingLastName}`.trim();
}

export function adminCustomerDocumentLabel(
  documentType: string,
  documentNumber: string,
): string {
  return `${documentType} ${documentNumber}`;
}

export function parseAdminCustomersPage(
  value: string | null | undefined,
): number {
  if (value == null || value.trim() === "") {
    return ADMIN_CUSTOMERS_DEFAULT_PAGE;
  }
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return ADMIN_CUSTOMERS_DEFAULT_PAGE;
  }
  return parsed;
}

export function parseAdminCustomerStatusFilter(
  value: string | null | undefined,
): AdminCustomerRecordStatusFilter {
  if (value === "ACTIVE" || value === "INACTIVE" || value === "ALL") {
    return value;
  }
  return "ALL";
}

export function parseAdminCustomerSortBy(
  value: string | null | undefined,
): AdminCustomerSortBy {
  if (
    value === "CREATED_AT" ||
    value === "NAME" ||
    value === "DOCUMENT" ||
    value === "ORDERS" ||
    value === "TOTAL_SPENT" ||
    value === "LAST_ORDER_AT"
  ) {
    return value;
  }
  return ADMIN_CUSTOMERS_DEFAULT_SORT_BY;
}

export function parseAdminCustomerSortDir(
  value: string | null | undefined,
): AdminCustomerSortDir {
  if (value === "ASC" || value === "DESC") {
    return value;
  }
  return ADMIN_CUSTOMERS_DEFAULT_SORT_DIR;
}

export function parseAdminCustomerHasPurchases(
  value: string | null | undefined,
): boolean | undefined {
  if (value == null || value.trim() === "") {
    return undefined;
  }
  const normalized = value.trim().toLowerCase();
  if (normalized === "true" || normalized === "yes" || normalized === "1") {
    return true;
  }
  if (normalized === "false" || normalized === "no" || normalized === "0") {
    return false;
  }
  return undefined;
}

export function adminCustomersSortSelectValue(
  sortBy: AdminCustomerSortBy,
  sortDir: AdminCustomerSortDir,
): string {
  const match = ADMIN_CUSTOMERS_SORT_OPTIONS.find(
    (option) => option.sortBy === sortBy && option.sortDir === sortDir,
  );
  return match?.value ?? `${sortBy}:${sortDir}`;
}

export function adminCustomersSortFromSelectValue(value: string): {
  sortBy: AdminCustomerSortBy;
  sortDir: AdminCustomerSortDir;
} {
  const match = ADMIN_CUSTOMERS_SORT_OPTIONS.find(
    (option) => option.value === value,
  );
  if (match) {
    return { sortBy: match.sortBy, sortDir: match.sortDir };
  }
  const [sortByRaw, sortDirRaw] = value.split(":");
  return {
    sortBy: parseAdminCustomerSortBy(sortByRaw),
    sortDir: parseAdminCustomerSortDir(sortDirRaw),
  };
}

export function adminCustomersPerspectiveParams(
  perspective: AdminCustomersPerspective,
): Pick<
  ListAdminCustomersQuery,
  "sortBy" | "sortDir" | "hasPurchases"
> {
  switch (perspective) {
    case "recent":
      return {
        sortBy: "CREATED_AT",
        sortDir: "DESC",
        hasPurchases: undefined,
      };
    case "activity":
      return {
        sortBy: "ORDERS",
        sortDir: "DESC",
        hasPurchases: undefined,
      };
    case "spend":
      return {
        sortBy: "TOTAL_SPENT",
        sortDir: "DESC",
        hasPurchases: undefined,
      };
    case "no_purchases":
      return {
        sortBy: ADMIN_CUSTOMERS_DEFAULT_SORT_BY,
        sortDir: ADMIN_CUSTOMERS_DEFAULT_SORT_DIR,
        hasPurchases: false,
      };
    case "all":
    default:
      return {
        sortBy: ADMIN_CUSTOMERS_DEFAULT_SORT_BY,
        sortDir: ADMIN_CUSTOMERS_DEFAULT_SORT_DIR,
        hasPurchases: undefined,
      };
  }
}

/** Inferencia de chip activo a partir de los params del listado. */
export function resolveAdminCustomersPerspective(query: {
  sortBy: AdminCustomerSortBy;
  sortDir: AdminCustomerSortDir;
  hasPurchases?: boolean;
}): AdminCustomersPerspective {
  if (query.hasPurchases === false) {
    return "no_purchases";
  }
  if (query.sortBy === "ORDERS" && query.sortDir === "DESC") {
    return "activity";
  }
  if (query.sortBy === "TOTAL_SPENT" && query.sortDir === "DESC") {
    return "spend";
  }
  return "all";
}

/**
 * "Todos" y "Registrados recientemente" comparten CREATED_AT DESC.
 * Solo se marca "recent" cuando el usuario eligió ese chip (`perspective=recent`).
 */
export function activeAdminCustomersPerspective(
  query: {
    sortBy: AdminCustomerSortBy;
    sortDir: AdminCustomerSortDir;
    hasPurchases?: boolean;
  },
  explicit: string | null | undefined,
): AdminCustomersPerspective {
  const inferred = resolveAdminCustomersPerspective(query);
  if (
    explicit === "recent" &&
    inferred === "all" &&
    query.sortBy === "CREATED_AT" &&
    query.sortDir === "DESC" &&
    query.hasPurchases !== false
  ) {
    return "recent";
  }
  return inferred;
}

export function adminCustomersListQueryFromSearchParams(params: {
  search?: string | null;
  status?: string | null;
  sortBy?: string | null;
  sortDir?: string | null;
  hasPurchases?: string | null;
  page?: string | null;
}): ListAdminCustomersQuery {
  return {
    search: params.search?.trim() || undefined,
    status: parseAdminCustomerStatusFilter(params.status),
    sortBy: parseAdminCustomerSortBy(params.sortBy),
    sortDir: parseAdminCustomerSortDir(params.sortDir),
    hasPurchases: parseAdminCustomerHasPurchases(params.hasPurchases),
    page: parseAdminCustomersPage(params.page),
    size: ADMIN_CUSTOMERS_DEFAULT_SIZE,
  };
}

export function adminCustomersHref(query: {
  search?: string;
  status?: AdminCustomerRecordStatusFilter;
  sortBy?: AdminCustomerSortBy;
  sortDir?: AdminCustomerSortDir;
  hasPurchases?: boolean;
  page?: number;
  perspective?: AdminCustomersPerspective;
}): string {
  const search = new URLSearchParams();
  const text = query.search?.trim() ?? "";
  if (text) {
    search.set("search", text);
  }
  const status = query.status ?? "ALL";
  if (status !== "ALL") {
    search.set("status", status);
  }
  const sortBy = query.sortBy ?? ADMIN_CUSTOMERS_DEFAULT_SORT_BY;
  const sortDir = query.sortDir ?? ADMIN_CUSTOMERS_DEFAULT_SORT_DIR;
  if (
    sortBy !== ADMIN_CUSTOMERS_DEFAULT_SORT_BY ||
    sortDir !== ADMIN_CUSTOMERS_DEFAULT_SORT_DIR
  ) {
    search.set("sortBy", sortBy);
    search.set("sortDir", sortDir);
  }
  if (query.hasPurchases !== undefined) {
    search.set("hasPurchases", query.hasPurchases ? "true" : "false");
  }
  const page = query.page ?? ADMIN_CUSTOMERS_DEFAULT_PAGE;
  if (page > ADMIN_CUSTOMERS_DEFAULT_PAGE) {
    search.set("page", String(page));
  }
  if (query.perspective === "recent") {
    search.set("perspective", "recent");
  }
  const encoded = search.toString();
  return encoded ? `/admin/customers?${encoded}` : "/admin/customers";
}

export function adminCustomerDetailHref(customerRecordId: string): string {
  return `/admin/customers/${encodeURIComponent(customerRecordId)}`;
}

export function adminCustomersPageCount(
  totalElements: number,
  size: number,
): number {
  if (size <= 0 || totalElements <= 0) {
    return 0;
  }
  return Math.ceil(totalElements / size);
}

export function canGoToPreviousAdminCustomersPage(page: number): boolean {
  return page > 0;
}

export function canGoToNextAdminCustomersPage(
  page: number,
  size: number,
  totalElements: number,
): boolean {
  return (page + 1) * size < totalElements;
}

export type AdminCustomerAccountsSummary = {
  total: number;
  active: number;
  inactive: number;
  deleted: number;
};

export function adminCustomerAccountsSummary(
  accounts: readonly {
    status: "ACTIVE" | "INACTIVE";
    deletedAt: string | null;
  }[],
): AdminCustomerAccountsSummary {
  const summary = {
    total: accounts.length,
    active: 0,
    inactive: 0,
    deleted: 0,
  };
  for (const account of accounts) {
    const status = adminCustomerAccountStatus(account);
    if (status === "ACTIVE") summary.active++;
    else if (status === "INACTIVE") summary.inactive++;
    else summary.deleted++;
  }
  return summary;
}

function accountCountLabel(
  count: number,
  singular: string,
  plural: string,
): string {
  return count === 1 ? `${count} ${singular}` : `${count} ${plural}`;
}

/** "2 activas · 1 inactiva · 1 eliminada" (omite ceros). */
export function adminCustomerAccountsSummaryLabel(
  summary: AdminCustomerAccountsSummary,
): string {
  const parts: string[] = [];
  if (summary.active > 0) {
    parts.push(accountCountLabel(summary.active, "activa", "activas"));
  }
  if (summary.inactive > 0) {
    parts.push(accountCountLabel(summary.inactive, "inactiva", "inactivas"));
  }
  if (summary.deleted > 0) {
    parts.push(accountCountLabel(summary.deleted, "eliminada", "eliminadas"));
  }
  return parts.length > 0 ? parts.join(" · ") : "Sin cuentas";
}

export function adminCustomerOrderStatusToneClass(
  status: OrderStatus,
): string {
  if (status === "DELIVERED") return "bg-emerald-50 text-emerald-700";
  if (status === "CANCELLED") return "bg-red-50 text-red-700";
  return "bg-amber-50 text-amber-700";
}

export function adminCustomerPaymentStatusLabel(
  status: PaymentStatus,
): string {
  if (status === "APPROVED") return "Aprobado";
  if (status === "DECLINED") return "Rechazado";
  return "Pendiente";
}

export function adminCustomerPaymentStatusToneClass(
  status: PaymentStatus,
): string {
  if (status === "APPROVED") return "bg-emerald-50 text-emerald-700";
  if (status === "DECLINED") return "bg-red-50 text-red-700";
  return "bg-amber-50 text-amber-700";
}

/** "Mostrando 1 a 10 de 23 pedidos" (con singular/plural). */
export function adminCustomerPageRangeLabel(
  page: number,
  size: number,
  totalElements: number,
  singular: string,
  plural: string,
): string {
  if (totalElements <= 0) {
    return `0 ${plural}`;
  }
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return `Mostrando ${from} a ${to} de ${totalElements} ${
    totalElements === 1 ? singular : plural
  }`;
}
