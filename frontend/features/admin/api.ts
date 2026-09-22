import type {
  Category,
  CategoryStatus,
  Product,
  ProductStatus,
} from "@/features/catalog/api";
import type {
  Order,
  OrderStatus,
  PagedOrders,
} from "@/features/orders/api";
import { COP, type Money } from "@/shared/money/money";
import { request } from "@/shared/api/client";

export type {
  Category,
  CategoryStatus,
  Order,
  OrderStatus,
  PagedOrders,
  Product,
  ProductStatus,
};

/** Defaults mirrored from Orders PageRequest (Application). */
export const ADMIN_ORDERS_DEFAULT_PAGE = 0;
export const ADMIN_ORDERS_DEFAULT_SIZE = 20;

export const ADMIN_ORDER_STATUSES: readonly OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PREPARING",
  "READY",
  "DELIVERED",
  "CANCELLED",
] as const;

export const ADMIN_CATALOG_VIEW = "ADMIN";

/** Mirrors CreateCategoryRequest. */
export type CreateAdminCategoryRequest = {
  name: string;
  description: string | null;
};

/** Mirrors UpdateCategoryRequest. Does not include status. */
export type UpdateAdminCategoryRequest = {
  name: string;
  description: string | null;
};

/** Mirrors CreateProductRequest. */
export type CreateAdminProductRequest = {
  categoryId: string;
  barcode: string | null;
  name: string;
  brand: string | null;
  description: string | null;
  price: Money;
  stock: number;
  imageUrl: string | null;
};

/** Mirrors UpdateProductRequest. Does not include price, stock, or status. */
export type UpdateAdminProductRequest = {
  categoryId: string;
  barcode: string | null;
  name: string;
  brand: string | null;
  description: string | null;
  imageUrl: string | null;
};

/** Mirrors ChangeProductPriceRequest. */
export type ChangeAdminProductPriceRequest = {
  price: Money;
};

export type ListAdminProductsQuery = {
  categoryId?: string;
  status?: ProductStatus;
};

export type SearchAdminProductsQuery = {
  text: string;
};

/** Query for GET /api/v1/admin/orders. */
export type ListAdminOrdersQuery = {
  page: number;
  size: number;
  status?: OrderStatus;
};

export function adminKeys() {
  return {
    all: ["admin"] as const,
    categoriesRoot: () => ["admin", "categories"] as const,
    categories: () => ["admin", "categories", "list"] as const,
    category: (categoryId: string) => ["admin", "category", categoryId] as const,
    productsRoot: () => ["admin", "products"] as const,
    products: (query: ListAdminProductsQuery = {}) =>
      ["admin", "products", "list", query.categoryId ?? "all", query.status ?? "all"] as const,
    search: (text: string) => ["admin", "products", "search", text] as const,
    product: (productId: string) => ["admin", "product", productId] as const,
    ordersRoot: () => ["admin", "orders"] as const,
    orders: (query: ListAdminOrdersQuery) =>
      [
        "admin",
        "orders",
        "list",
        query.page,
        query.size,
        query.status ?? "all",
      ] as const,
  };
}

export async function listAdminCategories(): Promise<Category[]> {
  return request<Category[]>(`/categories${toQuery({ view: ADMIN_CATALOG_VIEW })}`);
}

export async function getAdminCategory(categoryId: string): Promise<Category> {
  return request<Category>(
    `/categories/${encodeURIComponent(categoryId)}${toQuery({ view: ADMIN_CATALOG_VIEW })}`,
  );
}

export async function createAdminCategory(
  body: CreateAdminCategoryRequest,
): Promise<Category> {
  return request<Category>("/categories", {
    method: "POST",
    body,
  });
}

export async function updateAdminCategory(
  categoryId: string,
  body: UpdateAdminCategoryRequest,
): Promise<Category> {
  return request<Category>(`/categories/${encodeURIComponent(categoryId)}`, {
    method: "PUT",
    body,
  });
}

export async function activateAdminCategory(categoryId: string): Promise<Category> {
  return request<Category>(
    `/categories/${encodeURIComponent(categoryId)}/activate`,
    { method: "POST" },
  );
}

export async function deactivateAdminCategory(categoryId: string): Promise<Category> {
  return request<Category>(
    `/categories/${encodeURIComponent(categoryId)}/deactivate`,
    { method: "POST" },
  );
}

export async function listAdminProducts(
  query: ListAdminProductsQuery = {},
): Promise<Product[]> {
  return request<Product[]>(
    `/products${toQuery({
      view: ADMIN_CATALOG_VIEW,
      categoryId: query.categoryId,
      status: query.status,
    })}`,
  );
}

export async function searchAdminProducts(
  query: SearchAdminProductsQuery,
): Promise<Product[]> {
  return request<Product[]>(
    `/products/search${toQuery({
      view: ADMIN_CATALOG_VIEW,
      text: query.text,
    })}`,
  );
}

export async function getAdminProduct(productId: string): Promise<Product> {
  return request<Product>(
    `/products/${encodeURIComponent(productId)}${toQuery({ view: ADMIN_CATALOG_VIEW })}`,
  );
}

export async function createAdminProduct(
  body: CreateAdminProductRequest,
): Promise<Product> {
  return request<Product>("/products", {
    method: "POST",
    body,
  });
}

export async function updateAdminProduct(
  productId: string,
  body: UpdateAdminProductRequest,
): Promise<Product> {
  return request<Product>(`/products/${encodeURIComponent(productId)}`, {
    method: "PUT",
    body,
  });
}

export async function changeAdminProductPrice(
  productId: string,
  body: ChangeAdminProductPriceRequest,
): Promise<Product> {
  return request<Product>(`/products/${encodeURIComponent(productId)}/price`, {
    method: "POST",
    body,
  });
}

export async function activateAdminProduct(productId: string): Promise<Product> {
  return request<Product>(
    `/products/${encodeURIComponent(productId)}/activate`,
    { method: "POST" },
  );
}

export async function deactivateAdminProduct(productId: string): Promise<Product> {
  return request<Product>(
    `/products/${encodeURIComponent(productId)}/deactivate`,
    { method: "POST" },
  );
}

/** GET /api/v1/admin/orders — ADMIN; no reutilizar GET /api/v1/orders. */
export async function listAdminOrders(
  query: ListAdminOrdersQuery,
): Promise<PagedOrders> {
  return request<PagedOrders>(
    `/admin/orders${toQuery({
      page: String(query.page),
      size: String(query.size),
      status: query.status,
    })}`,
  );
}

export function copMoney(amount: number): Money {
  return { amount, currency: COP };
}

function toQuery(params: Record<string, string | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value.trim() !== "") {
      search.set(key, value);
    }
  }
  const encoded = search.toString();
  return encoded ? `?${encoded}` : "";
}
