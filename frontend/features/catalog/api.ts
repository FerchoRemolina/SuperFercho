import { request } from "@/shared/api/client";
import type { Money } from "@/shared/money/money";

export type CategoryStatus = "ACTIVE" | "INACTIVE";
export type ProductStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";

/** Mirrors CategoryRestResponse for GET /api/v1/categories. */
export type Category = {
  id: string;
  name: string;
  description: string | null;
  status: CategoryStatus;
  createdAt: string;
  updatedAt: string;
};

/** Mirrors ProductRestResponse for GET /api/v1/products. */
export type Product = {
  id: string;
  categoryId: string;
  barcode: string | null;
  name: string;
  brand: string | null;
  description: string | null;
  price: Money;
  stock: number;
  imageUrl: string | null;
  status: ProductStatus;
  createdAt: string;
  updatedAt: string;
};

export type ListProductsQuery = {
  categoryId?: string;
};

export type SearchProductsQuery = {
  text: string;
};

const anonymousGet = { anonymous: true as const };

export function catalogKeys() {
  return {
    all: ["catalog"] as const,
    categories: () => ["catalog", "categories"] as const,
    category: (categoryId: string) => ["catalog", "category", categoryId] as const,
    products: (categoryId?: string) =>
      ["catalog", "products", categoryId ?? "all"] as const,
    product: (productId: string) => ["catalog", "product", productId] as const,
    search: (text: string) => ["catalog", "search", text] as const,
  };
}

export async function listCategories(): Promise<Category[]> {
  return request<Category[]>("/categories", anonymousGet);
}

export async function getCategory(categoryId: string): Promise<Category> {
  return request<Category>(`/categories/${encodeURIComponent(categoryId)}`, anonymousGet);
}

export async function listProducts(query: ListProductsQuery = {}): Promise<Product[]> {
  return request<Product[]>(`/products${toQuery({ categoryId: query.categoryId })}`, anonymousGet);
}

export async function searchProducts(query: SearchProductsQuery): Promise<Product[]> {
  return request<Product[]>(`/products/search${toQuery({ text: query.text })}`, anonymousGet);
}

export async function getProduct(productId: string): Promise<Product> {
  return request<Product>(`/products/${encodeURIComponent(productId)}`, anonymousGet);
}

export function isProductAvailable(product: Product): boolean {
  return product.stock > 0;
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
