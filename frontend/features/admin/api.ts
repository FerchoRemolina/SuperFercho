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
  PaymentMethod,
  PaymentStatus,
} from "@/features/orders/api";
import { COP, type Money } from "@/shared/money/money";
import { request } from "@/shared/api/client";

export type {
  Category,
  CategoryStatus,
  Order,
  OrderStatus,
  PagedOrders,
  PaymentMethod,
  PaymentStatus,
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

/** Blueprint Ventas filter: excludes PENDING and CANCELLED. */
export const ADMIN_SALES_ORDER_STATUSES: readonly OrderStatus[] = [
  "CONFIRMED",
  "PREPARING",
  "READY",
  "DELIVERED",
] as const;

export const ADMIN_CATALOG_VIEW = "ADMIN";

/** Mirrors PresentationUnit (Catalog domain). */
export type PresentationUnit =
  | "G"
  | "KG"
  | "ML"
  | "L"
  | "UNIT"
  | "PACK"
  | "BOX"
  | "ROLL";

export const PRESENTATION_UNITS: readonly PresentationUnit[] = [
  "G",
  "KG",
  "ML",
  "L",
  "UNIT",
  "PACK",
  "BOX",
  "ROLL",
] as const;

/** Mirrors Presentation VO / REST body. quantity > 0, at most 3 decimal places. */
export type Presentation = {
  quantity: number;
  unit: PresentationUnit;
};

export type ProductTypeStatus = "ACTIVE" | "INACTIVE";

/** Mirrors ProductTypeRestResponse. */
export type ProductType = {
  id: string;
  categoryId: string;
  name: string;
  description: string | null;
  status: ProductTypeStatus;
  createdAt: string;
  updatedAt: string;
};

/** Mirrors CreateProductTypeRequest. */
export type CreateAdminProductTypeRequest = {
  categoryId: string;
  name: string;
  description: string | null;
};

/** Mirrors UpdateProductTypeRequest. Does not include categoryId or status. */
export type UpdateAdminProductTypeRequest = {
  name: string;
  description: string | null;
};

export type ProductVariantStatus = "ACTIVE" | "INACTIVE";

/** Mirrors ProductVariantRestResponse. */
export type ProductVariant = {
  id: string;
  productTypeId: string;
  name: string;
  description: string | null;
  status: ProductVariantStatus;
  createdAt: string;
  updatedAt: string;
};

/** Mirrors CreateProductVariantRequest. */
export type CreateAdminProductVariantRequest = {
  productTypeId: string;
  name: string;
  description: string | null;
};

/** Mirrors UpdateProductVariantRequest. Does not include productTypeId or status. */
export type UpdateAdminProductVariantRequest = {
  name: string;
  description: string | null;
};

/**
 * Admin Product view of ProductRestResponse (includes taxonomy + presentation).
 * Customer catalog Product type stays narrower on purpose.
 */
export type AdminProduct = {
  id: string;
  categoryId: string;
  productTypeId: string;
  productVariantId: string | null;
  presentation: Presentation;
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

/** Mirrors CreateProductRequest. Does not accept categoryId. */
export type CreateAdminProductRequest = {
  productTypeId: string;
  productVariantId: string | null;
  presentation: Presentation;
  barcode: string | null;
  name: string;
  brand: string | null;
  description: string | null;
  price: Money;
  stock: number;
  imageUrl: string | null;
};

/** Mirrors UpdateProductRequest. Does not include price, stock, status, or categoryId. */
export type UpdateAdminProductRequest = {
  productTypeId: string;
  productVariantId: string | null;
  presentation: Presentation;
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

/** Mirrors AdjustProductStockRequest. */
export type AdjustAdminProductStockRequest = {
  stock: number;
};

export type ListAdminProductsQuery = {
  categoryId?: string;
  status?: ProductStatus;
};

export type SearchAdminProductsQuery = {
  text: string;
};

export type ListAdminProductTypesQuery = {
  categoryId: string;
};

export type ListAdminProductVariantsQuery = {
  productTypeId: string;
};

/** Query for GET /api/v1/admin/orders. */
export type ListAdminOrdersQuery = {
  page: number;
  size: number;
  /** One status, or the Ventas multi-status set. */
  status?: OrderStatus | readonly OrderStatus[];
};

export function adminOrdersStatusQueryKey(
  status: ListAdminOrdersQuery["status"],
): string {
  if (status == null) {
    return "all";
  }
  if (typeof status === "string") {
    return status;
  }
  return isAdminSalesOrderStatuses(status) ? "sales" : status.join(",");
}

export function isAdminSalesOrderStatuses(
  statuses: readonly OrderStatus[],
): boolean {
  if (statuses.length !== ADMIN_SALES_ORDER_STATUSES.length) {
    return false;
  }
  const unique = new Set(statuses);
  if (unique.size !== ADMIN_SALES_ORDER_STATUSES.length) {
    return false;
  }
  return ADMIN_SALES_ORDER_STATUSES.every((status) => unique.has(status));
}

export function serializeAdminOrdersStatusParam(
  status: ListAdminOrdersQuery["status"],
): string | undefined {
  if (status == null) {
    return undefined;
  }
  if (typeof status === "string") {
    return status;
  }
  if (isAdminSalesOrderStatuses(status)) {
    return ADMIN_SALES_ORDER_STATUSES.join(",");
  }
  return undefined;
}

export function adminKeys() {
  return {
    all: ["admin"] as const,
    categoriesRoot: () => ["admin", "categories"] as const,
    categories: () => ["admin", "categories", "list"] as const,
    category: (categoryId: string) => ["admin", "category", categoryId] as const,
    productTypesRoot: () => ["admin", "product-types"] as const,
    productTypes: (categoryId: string) =>
      ["admin", "product-types", "list", categoryId] as const,
    productType: (productTypeId: string) =>
      ["admin", "product-type", productTypeId] as const,
    productVariantsRoot: () => ["admin", "product-variants"] as const,
    productVariants: (productTypeId: string) =>
      ["admin", "product-variants", "list", productTypeId] as const,
    productVariant: (productVariantId: string) =>
      ["admin", "product-variant", productVariantId] as const,
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
        adminOrdersStatusQueryKey(query.status),
      ] as const,
    order: (orderId: string) => ["admin", "order", orderId] as const,
    knowledgeRoot: () => ["admin", "knowledge"] as const,
    knowledgeDocuments: () => ["admin", "knowledge", "documents"] as const,
    knowledgeDocument: (documentId: string) =>
      ["admin", "knowledge", "document", documentId] as const,
    knowledgeSearch: (query: string, limit: number) =>
      ["admin", "knowledge", "search", query, limit] as const,
    paymentsRoot: () => ["admin", "payments"] as const,
    payment: (paymentId: string) => ["admin", "payment", paymentId] as const,
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
): Promise<AdminProduct[]> {
  return request<AdminProduct[]>(
    `/products${toQuery({
      view: ADMIN_CATALOG_VIEW,
      categoryId: query.categoryId,
      status: query.status,
    })}`,
  );
}

export async function searchAdminProducts(
  query: SearchAdminProductsQuery,
): Promise<AdminProduct[]> {
  return request<AdminProduct[]>(
    `/products/search${toQuery({
      view: ADMIN_CATALOG_VIEW,
      text: query.text,
    })}`,
  );
}

export async function getAdminProduct(productId: string): Promise<AdminProduct> {
  return request<AdminProduct>(
    `/products/${encodeURIComponent(productId)}${toQuery({ view: ADMIN_CATALOG_VIEW })}`,
  );
}

export async function createAdminProduct(
  body: CreateAdminProductRequest,
): Promise<AdminProduct> {
  return request<AdminProduct>("/products", {
    method: "POST",
    body,
  });
}

export async function updateAdminProduct(
  productId: string,
  body: UpdateAdminProductRequest,
): Promise<AdminProduct> {
  return request<AdminProduct>(`/products/${encodeURIComponent(productId)}`, {
    method: "PUT",
    body,
  });
}

export async function changeAdminProductPrice(
  productId: string,
  body: ChangeAdminProductPriceRequest,
): Promise<AdminProduct> {
  return request<AdminProduct>(`/products/${encodeURIComponent(productId)}/price`, {
    method: "POST",
    body,
  });
}

/** POST /api/v1/products/{productId}/stock — ADMIN. */
export async function adjustAdminProductStock(
  productId: string,
  body: AdjustAdminProductStockRequest,
): Promise<AdminProduct> {
  return request<AdminProduct>(`/products/${encodeURIComponent(productId)}/stock`, {
    method: "POST",
    body,
  });
}

export async function activateAdminProduct(productId: string): Promise<AdminProduct> {
  return request<AdminProduct>(
    `/products/${encodeURIComponent(productId)}/activate`,
    { method: "POST" },
  );
}

export async function deactivateAdminProduct(productId: string): Promise<AdminProduct> {
  return request<AdminProduct>(
    `/products/${encodeURIComponent(productId)}/deactivate`,
    { method: "POST" },
  );
}

export async function archiveAdminProduct(productId: string): Promise<AdminProduct> {
  return request<AdminProduct>(
    `/products/${encodeURIComponent(productId)}/archive`,
    { method: "POST" },
  );
}

export async function restoreAdminProduct(productId: string): Promise<AdminProduct> {
  return request<AdminProduct>(
    `/products/${encodeURIComponent(productId)}/restore`,
    { method: "POST" },
  );
}

export async function listAdminProductTypes(
  query: ListAdminProductTypesQuery,
): Promise<ProductType[]> {
  return request<ProductType[]>(
    `/product-types${toQuery({ categoryId: query.categoryId })}`,
  );
}

export async function getAdminProductType(productTypeId: string): Promise<ProductType> {
  return request<ProductType>(`/product-types/${encodeURIComponent(productTypeId)}`);
}

export async function createAdminProductType(
  body: CreateAdminProductTypeRequest,
): Promise<ProductType> {
  return request<ProductType>("/product-types", {
    method: "POST",
    body,
  });
}

export async function updateAdminProductType(
  productTypeId: string,
  body: UpdateAdminProductTypeRequest,
): Promise<ProductType> {
  return request<ProductType>(`/product-types/${encodeURIComponent(productTypeId)}`, {
    method: "PUT",
    body,
  });
}

export async function activateAdminProductType(
  productTypeId: string,
): Promise<ProductType> {
  return request<ProductType>(
    `/product-types/${encodeURIComponent(productTypeId)}/activate`,
    { method: "POST" },
  );
}

export async function deactivateAdminProductType(
  productTypeId: string,
): Promise<ProductType> {
  return request<ProductType>(
    `/product-types/${encodeURIComponent(productTypeId)}/deactivate`,
    { method: "POST" },
  );
}

export async function listAdminProductVariants(
  query: ListAdminProductVariantsQuery,
): Promise<ProductVariant[]> {
  return request<ProductVariant[]>(
    `/product-variants${toQuery({ productTypeId: query.productTypeId })}`,
  );
}

export async function getAdminProductVariant(
  productVariantId: string,
): Promise<ProductVariant> {
  return request<ProductVariant>(
    `/product-variants/${encodeURIComponent(productVariantId)}`,
  );
}

export async function createAdminProductVariant(
  body: CreateAdminProductVariantRequest,
): Promise<ProductVariant> {
  return request<ProductVariant>("/product-variants", {
    method: "POST",
    body,
  });
}

export async function updateAdminProductVariant(
  productVariantId: string,
  body: UpdateAdminProductVariantRequest,
): Promise<ProductVariant> {
  return request<ProductVariant>(
    `/product-variants/${encodeURIComponent(productVariantId)}`,
    {
      method: "PUT",
      body,
    },
  );
}

export async function activateAdminProductVariant(
  productVariantId: string,
): Promise<ProductVariant> {
  return request<ProductVariant>(
    `/product-variants/${encodeURIComponent(productVariantId)}/activate`,
    { method: "POST" },
  );
}

export async function deactivateAdminProductVariant(
  productVariantId: string,
): Promise<ProductVariant> {
  return request<ProductVariant>(
    `/product-variants/${encodeURIComponent(productVariantId)}/deactivate`,
    { method: "POST" },
  );
}

/** Mirrors BarcodeProductSuggestionRestResponse. ADMIN lookup only. */
export type BarcodeProductSuggestion = {
  barcode: string;
  name: string | null;
  brand: string | null;
  description: string | null;
  imageUrl: string | null;
};

/** GET /api/v1/products/barcode-lookup/{barcode} — ADMIN. Does not create a product. */
export async function lookupProductByBarcode(
  barcode: string,
): Promise<BarcodeProductSuggestion> {
  return request<BarcodeProductSuggestion>(
    `/products/barcode-lookup/${encodeURIComponent(barcode)}`,
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
      status: serializeAdminOrdersStatusParam(query.status),
    })}`,
  );
}

/** GET /api/v1/admin/orders/{orderId} — ADMIN; no reutilizar GET /api/v1/orders/{orderId}. */
export async function getAdminOrder(orderId: string): Promise<Order> {
  return request<Order>(`/admin/orders/${encodeURIComponent(orderId)}`);
}

/** Mirrors UpdateOrderStatusRequest for POST /api/v1/orders/{orderId}/status (ADMIN). */
export type UpdateAdminOrderStatusRequest = {
  status: OrderStatus;
};

/**
 * POST /api/v1/orders/{orderId}/status — ADMIN.
 * Response may include payment=null; prefer refreshing GET /admin/orders/{id}.
 */
export async function updateAdminOrderStatus(
  orderId: string,
  body: UpdateAdminOrderStatusRequest,
): Promise<Order> {
  return request<Order>(`/orders/${encodeURIComponent(orderId)}/status`, {
    method: "POST",
    body,
  });
}

/** Mirrors DocumentStatus. */
export type DocumentStatus =
  | "RECEIVED"
  | "CHUNKED"
  | "READY"
  | "FAILED"
  | "INACTIVE";

export const ADMIN_DOCUMENT_STATUSES: readonly DocumentStatus[] = [
  "RECEIVED",
  "CHUNKED",
  "READY",
  "FAILED",
  "INACTIVE",
] as const;

/** Mirrors ChunkRestResponse. */
export type KnowledgeChunk = {
  id: string;
  position: number;
  text: string;
  embedded: boolean;
};

/** Mirrors DocumentRestResponse. */
export type KnowledgeDocument = {
  id: string;
  title: string;
  source: string;
  content: string;
  status: DocumentStatus;
  chunks: KnowledgeChunk[];
  createdAt: string;
  updatedAt: string;
};

/** Mirrors CreateDocumentRequest. */
export type CreateAdminKnowledgeDocumentRequest = {
  title: string;
  source: string;
  content: string;
};

/** Mirrors ReplaceDocumentContentRequest. */
export type ReplaceAdminKnowledgeDocumentContentRequest = {
  content: string;
};

/** GET /api/v1/knowledge/documents — ADMIN. */
export async function listAdminKnowledgeDocuments(): Promise<
  KnowledgeDocument[]
> {
  return request<KnowledgeDocument[]>("/knowledge/documents");
}

/** GET /api/v1/knowledge/documents/{documentId} — ADMIN. */
export async function getAdminKnowledgeDocument(
  documentId: string,
): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(
    `/knowledge/documents/${encodeURIComponent(documentId)}`,
  );
}

/** POST /api/v1/knowledge/documents — ADMIN; 201. */
export async function createAdminKnowledgeDocument(
  body: CreateAdminKnowledgeDocumentRequest,
): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>("/knowledge/documents", {
    method: "POST",
    body,
  });
}

/** PUT /api/v1/knowledge/documents/{documentId}/content — ADMIN. */
export async function replaceAdminKnowledgeDocumentContent(
  documentId: string,
  body: ReplaceAdminKnowledgeDocumentContentRequest,
): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(
    `/knowledge/documents/${encodeURIComponent(documentId)}/content`,
    {
      method: "PUT",
      body,
    },
  );
}

/** POST /api/v1/knowledge/documents/{documentId}/process — ADMIN. */
export async function processAdminKnowledgeDocument(
  documentId: string,
): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(
    `/knowledge/documents/${encodeURIComponent(documentId)}/process`,
    { method: "POST" },
  );
}

/** POST /api/v1/knowledge/documents/{documentId}/deactivate — ADMIN. */
export async function deactivateAdminKnowledgeDocument(
  documentId: string,
): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(
    `/knowledge/documents/${encodeURIComponent(documentId)}/deactivate`,
    { method: "POST" },
  );
}

/** POST /api/v1/knowledge/documents/{documentId}/reactivate — ADMIN. */
export async function reactivateAdminKnowledgeDocument(
  documentId: string,
): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(
    `/knowledge/documents/${encodeURIComponent(documentId)}/reactivate`,
    { method: "POST" },
  );
}

/** Mirrors PaymentRestResponse from GET /api/v1/payments/{paymentId}. */
export type AdminPayment = {
  id: string;
  orderId: string;
  amount: Money;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  providerReference: string | null;
  createdAt: string;
  updatedAt: string;
  refundedAt: string | null;
};

export const ADMIN_PAYMENT_STATUSES: readonly PaymentStatus[] = [
  "PENDING",
  "APPROVED",
  "DECLINED",
] as const;

/** GET /api/v1/payments/{paymentId} — ADMIN. */
export async function getAdminPayment(paymentId: string): Promise<AdminPayment> {
  return request<AdminPayment>(`/payments/${encodeURIComponent(paymentId)}`);
}

/** Mirrors KnowledgeSearchHitRestResponse. */
export type KnowledgeSearchHit = {
  documentId: string;
  chunkId: string;
  title: string;
  source: string;
  chunkText: string;
  score: number;
};

/** Mirrors KnowledgeSearchRestResponse. */
export type KnowledgeSearchResult = {
  hits: KnowledgeSearchHit[];
};

/** Query for GET /api/v1/knowledge/search — limit is required by the controller (1–20). */
export type SearchAdminKnowledgeQuery = {
  query: string;
  limit: number;
};

export const ADMIN_KNOWLEDGE_SEARCH_MIN_LIMIT = 1;
export const ADMIN_KNOWLEDGE_SEARCH_MAX_LIMIT = 20;
export const ADMIN_KNOWLEDGE_SEARCH_DEFAULT_LIMIT = 10;

/** GET /api/v1/knowledge/search — ADMIN. */
export async function searchAdminKnowledge(
  query: SearchAdminKnowledgeQuery,
): Promise<KnowledgeSearchResult> {
  return request<KnowledgeSearchResult>(
    `/knowledge/search${toQuery({
      query: query.query,
      limit: String(query.limit),
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
