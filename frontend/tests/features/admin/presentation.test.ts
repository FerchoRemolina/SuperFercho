import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  adminKnowledgeDocumentHref,
  adminKnowledgeHref,
  adminKnowledgeNewHref,
  adminKnowledgeSearchQueryFromSearchParams,
  adminOrderDetailErrorKind,
  adminOrderDetailHref,
  adminOrdersFilterSelectValue,
  adminOrdersHref,
  adminOrdersListQueryFromSearchParams,
  adminOrdersPageCount,
  adminOrdersStatusFromSelectValue,
  adminOrderStatusSummary,
  adminPaymentDetailErrorKind,
  adminPaymentHref,
  adminProductsHref,
  adminProductsListHrefFromSearchParams,
  canActivateProduct,
  canArchiveProduct,
  canDeactivateKnowledgeDocument,
  canDeactivateProduct,
  canGoToNextAdminOrdersPage,
  canGoToPreviousAdminOrdersPage,
  canProcessKnowledgeDocument,
  canReactivateKnowledgeDocument,
  canReplaceKnowledgeDocumentContent,
  canRestoreProduct,
  documentStatusLabel,
  documentStatusTone,
  formatAdminInstant,
  formatAdminPresentation,
  isAdminRole,
  knowledgeDocumentDeactivateConfirmation,
  knowledgeDocumentProcessConfirmation,
  knowledgeDocumentReactivateConfirmation,
  knowledgeDocumentReplaceContentWarning,
  listQueryFromSearchParams,
  parseAdminKnowledgeSearchLimit,
  parseAdminOrderStatus,
  parseAdminOrdersPage,
  parseAdminOrdersStatusFilter,
  partitionAdminStockAttention,
  paymentStatusTone,
  productStatusLabel,
  productStatusTone,
  shouldSearchAdminKnowledge,
  shouldSearchAdminProducts,
  ADMIN_NAV_LINKS,
  ADMIN_STOCK_LOW_EMPTY_MESSAGE,
  ADMIN_STOCK_OUT_EMPTY_MESSAGE,
  adminProductDetailHref,
} from "@/features/admin/presentation";
import { productAvailabilityLabel } from "@/features/catalog/quantity";
import type { AdminProduct } from "@/features/admin/api";
import {
  ADMIN_DOCUMENT_STATUSES,
  ADMIN_SALES_ORDER_STATUSES,
} from "@/features/admin/api";
import { ApiError } from "@/shared/errors/api-problem";
import { formatMoney } from "@/shared/money/money";
import { messageForApiProblem } from "@/shared/errors/messages";

function adminProduct(
  overrides: Partial<AdminProduct> & Pick<AdminProduct, "id" | "name" | "stock">,
): AdminProduct {
  return {
    categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    productTypeId: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    productVariantId: null,
    presentation: { quantity: 1, unit: "UNIT" },
    barcode: null,
    brand: null,
    description: null,
    price: { amount: 1000, currency: "COP" },
    imageUrl: null,
    status: "ACTIVE",
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-03-01T10:30:00Z",
    ...overrides,
  };
}

describe("admin presentation", () => {
  it("detects admin role", () => {
    expect(isAdminRole("ADMIN")).toBe(true);
    expect(isAdminRole("CUSTOMER")).toBe(false);
    expect(isAdminRole(undefined)).toBe(false);
  });

  it("builds list href with filters when not searching", () => {
    expect(
      adminProductsHref({
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        status: "INACTIVE",
      }),
    ).toBe(
      "/admin/products?categoryId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa&status=INACTIVE",
    );
  });

  it("builds clean list href when there are no filters", () => {
    expect(adminProductsHref({})).toBe("/admin/products");
    expect(adminProductsHref({ text: "   " })).toBe("/admin/products");
    expect(
      adminProductsListHrefFromSearchParams({
        text: null,
        categoryId: null,
        status: null,
      }),
    ).toBe("/admin/products");
  });

  it("builds search href without category or status filters", () => {
    expect(
      adminProductsHref({
        text: "leche",
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        status: "ACTIVE",
      }),
    ).toBe("/admin/products?text=leche");
  });

  it("parses list query from search params", () => {
    expect(
      listQueryFromSearchParams({
        categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        status: "ACTIVE",
      }),
    ).toEqual({
      categoryId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      status: "ACTIVE",
    });
    expect(listQueryFromSearchParams({ status: "ARCHIVED" })).toEqual({
      status: "ARCHIVED",
    });
    expect(listQueryFromSearchParams({ status: "UNKNOWN" })).toEqual({});
  });

  it("preserves list filters on product detail href and back link", () => {
    const productId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    const categoryId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    expect(
      adminProductDetailHref(productId, {
        categoryId,
        status: "ACTIVE",
      }),
    ).toBe(
      `/admin/products/${productId}?categoryId=${categoryId}&status=ACTIVE`,
    );

    expect(
      adminProductDetailHref(productId, {
        text: "detergente",
        categoryId,
        status: "ACTIVE",
      }),
    ).toBe(`/admin/products/${productId}?text=detergente`);

    expect(adminProductDetailHref(productId)).toBe(
      `/admin/products/${productId}`,
    );

    expect(
      adminProductsListHrefFromSearchParams({
        text: null,
        categoryId,
        status: "ACTIVE",
      }),
    ).toBe(`/admin/products?categoryId=${categoryId}&status=ACTIVE`);

    expect(
      adminProductsListHrefFromSearchParams({
        text: "detergente",
        categoryId,
        status: "ACTIVE",
      }),
    ).toBe("/admin/products?text=detergente");
  });

  it("labels and tones product statuses", () => {
    expect(productStatusLabel("ACTIVE")).toBe("Activo");
    expect(productStatusLabel("INACTIVE")).toBe("Inactivo");
    expect(productStatusLabel("ARCHIVED")).toBe("Archivado");
    expect(productStatusTone("ACTIVE")).toBe("success");
    expect(productStatusTone("INACTIVE")).toBe("neutral");
    expect(productStatusTone("ARCHIVED")).toBe("danger");
  });

  it("does not label inactive or archived stock as Agotado", () => {
    expect(productAvailabilityLabel("ACTIVE", 0)).toBe("Agotado");
    expect(productAvailabilityLabel("ACTIVE", 3)).toBe("Disponible");
    expect(productAvailabilityLabel("INACTIVE", 0)).toBe("No disponible");
    expect(productAvailabilityLabel("INACTIVE", 8)).toBe("No disponible");
    expect(productAvailabilityLabel("ARCHIVED", 0)).toBe("Archivado");
  });

  it("partitions ACTIVE products for Hub stock attention", () => {
    const products: AdminProduct[] = [
      adminProduct({ id: "a", name: "Agua", stock: 1 }),
      adminProduct({ id: "b", name: "Leche", stock: 5 }),
      adminProduct({ id: "c", name: "Pan", stock: 6 }),
      adminProduct({ id: "d", name: "Huevos", stock: 0 }),
      adminProduct({
        id: "e",
        name: "Inactivo",
        stock: 2,
        status: "INACTIVE",
      }),
      adminProduct({
        id: "f",
        name: "Archivado",
        stock: 0,
        status: "ARCHIVED",
      }),
    ];

    const { lowStock, outOfStock } = partitionAdminStockAttention(products);

    expect(lowStock.map((product) => product.id)).toEqual(["a", "b"]);
    expect(outOfStock.map((product) => product.id)).toEqual(["d"]);
    expect(ADMIN_STOCK_LOW_EMPTY_MESSAGE).toMatch(/reposición/i);
    expect(ADMIN_STOCK_LOW_EMPTY_MESSAGE).not.toMatch(/1 a 5/);
    expect(ADMIN_STOCK_OUT_EMPTY_MESSAGE).toMatch(/agotados/i);
  });

  it("formats presentation and product detail href", () => {
    expect(formatAdminPresentation({ quantity: 1, unit: "UNIT" })).toBe(
      "1 UNIT",
    );
    expect(formatAdminPresentation({ quantity: 0.5, unit: "KG" })).toBe(
      "0.5 KG",
    );
    expect(
      adminProductDetailHref("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).toBe("/admin/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  });

  it("gates product status transitions", () => {
    expect(canActivateProduct("INACTIVE")).toBe(true);
    expect(canActivateProduct("ACTIVE")).toBe(false);
    expect(canActivateProduct("ARCHIVED")).toBe(false);
    expect(canDeactivateProduct("ACTIVE")).toBe(true);
    expect(canDeactivateProduct("INACTIVE")).toBe(false);
    expect(canDeactivateProduct("ARCHIVED")).toBe(false);
    expect(canArchiveProduct("ACTIVE")).toBe(true);
    expect(canArchiveProduct("INACTIVE")).toBe(true);
    expect(canArchiveProduct("ARCHIVED")).toBe(false);
    expect(canRestoreProduct("ARCHIVED")).toBe(true);
    expect(canRestoreProduct("ACTIVE")).toBe(false);
    expect(canRestoreProduct("INACTIVE")).toBe(false);
  });

  it("builds archived status filter href", () => {
    expect(adminProductsHref({ status: "ARCHIVED" })).toBe(
      "/admin/products?status=ARCHIVED",
    );
  });

  it("requires non-blank text to search", () => {
    expect(shouldSearchAdminProducts("  leche ")).toBe(true);
    expect(shouldSearchAdminProducts("   ")).toBe(false);
  });
});

describe("admin orders presentation", () => {
  it("parses known order statuses and rejects unknown ones", () => {
    expect(parseAdminOrderStatus("CONFIRMED")).toBe("CONFIRMED");
    expect(parseAdminOrderStatus("SOLD")).toBeUndefined();
    expect(parseAdminOrderStatus("")).toBeUndefined();
    expect(parseAdminOrderStatus(null)).toBeUndefined();
  });

  it("parses page from search params with default 0", () => {
    expect(parseAdminOrdersPage(null)).toBe(0);
    expect(parseAdminOrdersPage("2")).toBe(2);
    expect(parseAdminOrdersPage("-1")).toBe(0);
    expect(parseAdminOrdersPage("abc")).toBe(0);
  });

  it("builds list query with default size 20", () => {
    expect(
      adminOrdersListQueryFromSearchParams({
        page: "1",
        status: "PREPARING",
      }),
    ).toEqual({ page: 1, size: 20, status: "PREPARING" });
    expect(adminOrdersListQueryFromSearchParams({})).toEqual({
      page: 0,
      size: 20,
      status: undefined,
    });
  });

  it("parses Ventas multi-status regardless of token order", () => {
    expect(
      parseAdminOrdersStatusFilter("CONFIRMED,PREPARING,DELIVERY,DELIVERED"),
    ).toEqual(ADMIN_SALES_ORDER_STATUSES);
    expect(
      parseAdminOrdersStatusFilter("DELIVERED,DELIVERY,PREPARING,CONFIRMED"),
    ).toEqual(ADMIN_SALES_ORDER_STATUSES);
    expect(
      adminOrdersListQueryFromSearchParams({
        status: "CONFIRMED,PREPARING,DELIVERY,DELIVERED",
      }),
    ).toEqual({
      page: 0,
      size: 20,
      status: ADMIN_SALES_ORDER_STATUSES,
    });
  });

  it("rejects invalid tokens and non-Ventas multi-status sets", () => {
    expect(parseAdminOrdersStatusFilter("SOLD")).toBeUndefined();
    expect(
      parseAdminOrdersStatusFilter("CONFIRMED,PREPARING,DELIVERY,SOLD"),
    ).toBeUndefined();
    expect(
      parseAdminOrdersStatusFilter("CONFIRMED,PREPARING,DELIVERY"),
    ).toBeUndefined();
    expect(
      parseAdminOrdersStatusFilter("CANCELLED,CONFIRMED,PREPARING,DELIVERY,DELIVERED"),
    ).toBeUndefined();
  });

  it("builds orders href with page and status filters", () => {
    expect(adminOrdersHref({})).toBe("/admin/orders");
    expect(adminOrdersHref({ page: 0, status: "" })).toBe("/admin/orders");
    expect(adminOrdersHref({ page: 2, status: "DELIVERY" })).toBe(
      "/admin/orders?page=2&status=DELIVERY",
    );
    expect(adminOrdersHref({ page: 0, status: "CANCELLED" })).toBe(
      "/admin/orders?status=CANCELLED",
    );
    expect(
      adminOrdersHref({ page: 0, status: ADMIN_SALES_ORDER_STATUSES }),
    ).toBe(
      "/admin/orders?status=CONFIRMED%2CPREPARING%2CDELIVERY%2CDELIVERED",
    );
  });

  it("maps select values to and from Ventas and single statuses", () => {
    expect(adminOrdersFilterSelectValue(undefined)).toBe("");
    expect(adminOrdersFilterSelectValue("CONFIRMED")).toBe("CONFIRMED");
    expect(adminOrdersFilterSelectValue(ADMIN_SALES_ORDER_STATUSES)).toBe(
      "sales",
    );
    expect(adminOrdersStatusFromSelectValue("")).toBe("");
    expect(adminOrdersStatusFromSelectValue("sales")).toEqual(
      ADMIN_SALES_ORDER_STATUSES,
    );
    expect(adminOrdersStatusFromSelectValue("DELIVERY")).toBe("DELIVERY");
    expect(adminOrdersStatusFromSelectValue("SOLD")).toBe("");
  });

  it("computes pagination bounds from totalElements", () => {
    expect(adminOrdersPageCount(0, 20)).toBe(0);
    expect(adminOrdersPageCount(20, 20)).toBe(1);
    expect(adminOrdersPageCount(21, 20)).toBe(2);
    expect(canGoToPreviousAdminOrdersPage(0)).toBe(false);
    expect(canGoToPreviousAdminOrdersPage(1)).toBe(true);
    expect(canGoToNextAdminOrdersPage(0, 20, 20)).toBe(false);
    expect(canGoToNextAdminOrdersPage(0, 20, 21)).toBe(true);
    expect(canGoToNextAdminOrdersPage(1, 20, 21)).toBe(false);
  });

  it("builds admin order detail href", () => {
    expect(
      adminOrderDetailHref("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).toBe("/admin/orders/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  });

  it("classifies detail errors without collapsing PAYMENT_NOT_FOUND", () => {
    expect(
      adminOrderDetailErrorKind(
        new ApiError({
          status: 404,
          code: "ORDER_NOT_FOUND",
          title: "Not Found",
        }),
      ),
    ).toBe("order_not_found");
    expect(
      adminOrderDetailErrorKind(
        new ApiError({
          status: 404,
          code: "PAYMENT_NOT_FOUND",
          title: "Not Found",
        }),
      ),
    ).toBe("payment_not_found");
    expect(adminOrderDetailErrorKind(new Error("boom"))).toBe("error");
  });

  it("formats money and admin instants for detail display", () => {
    expect(formatMoney({ amount: 9000, currency: "COP" })).toMatch(/9\.000/);
    expect(formatAdminInstant("2026-03-01T15:00:00Z")).toMatch(/2026/);
  });
});

describe("admin order status", () => {
  it("describes the status as read-only because the lifecycle is automatic", () => {
    expect(adminOrderStatusSummary("CONFIRMED")).toBe(
      "Estado actual: Confirmado. El pedido avanza automáticamente.",
    );
    expect(adminOrderStatusSummary("DELIVERY")).toContain("En camino");
    expect(adminOrderStatusSummary("DELIVERED")).toContain(
      "completó su ciclo de vida",
    );
    expect(adminOrderStatusSummary("CANCELLED")).toBe(
      "Estado actual: Cancelado.",
    );
    expect(adminOrderStatusSummary("CONFIRMED")).not.toMatch(/cambiar/i);
  });

  it("no longer ships a manual status advance component", () => {
    expect(
      existsSync(
        join(
          dirname(fileURLToPath(import.meta.url)),
          "../../../features/admin/components/admin-order-status-actions.tsx",
        ),
      ),
    ).toBe(false);
  });

  it("maps order problem codes without cancel-oriented wording", () => {
    expect(
      messageForApiProblem({
        status: 404,
        code: "ORDER_NOT_FOUND",
        detail: "Order not found",
      }),
    ).toBe("No encontramos ese pedido.");
    expect(
      messageForApiProblem({
        status: 409,
        code: "INVALID_ORDER_TRANSITION",
        detail: "Invalid order state transition: CONFIRMED -> PREPARING",
      }),
    ).toBe("El pedido ya no está en un estado que permita esta acción.");
  });
});

describe("admin knowledge presentation", () => {
  it("labels every document status", () => {
    expect(documentStatusLabel("RECEIVED")).toBe("Recibido");
    expect(documentStatusLabel("CHUNKED")).toBe("Fragmentado");
    expect(documentStatusLabel("READY")).toBe("Listo");
    expect(documentStatusLabel("FAILED")).toBe("Fallido");
    expect(documentStatusLabel("INACTIVE")).toBe("Inactivo");
  });

  it("allows process only for RECEIVED CHUNKED and FAILED", () => {
    expect(canProcessKnowledgeDocument("RECEIVED")).toBe(true);
    expect(canProcessKnowledgeDocument("CHUNKED")).toBe(true);
    expect(canProcessKnowledgeDocument("FAILED")).toBe(true);
    expect(canProcessKnowledgeDocument("READY")).toBe(false);
    expect(canProcessKnowledgeDocument("INACTIVE")).toBe(false);
  });

  it("allows deactivate only for READY", () => {
    expect(canDeactivateKnowledgeDocument("READY")).toBe(true);
    expect(canDeactivateKnowledgeDocument("RECEIVED")).toBe(false);
    expect(canDeactivateKnowledgeDocument("CHUNKED")).toBe(false);
    expect(canDeactivateKnowledgeDocument("FAILED")).toBe(false);
    expect(canDeactivateKnowledgeDocument("INACTIVE")).toBe(false);
  });

  it("allows reactivate only for INACTIVE", () => {
    expect(canReactivateKnowledgeDocument("INACTIVE")).toBe(true);
    expect(canReactivateKnowledgeDocument("READY")).toBe(false);
    expect(canReactivateKnowledgeDocument("RECEIVED")).toBe(false);
    expect(canReactivateKnowledgeDocument("CHUNKED")).toBe(false);
    expect(canReactivateKnowledgeDocument("FAILED")).toBe(false);
  });

  it("allows replace content for every known document status", () => {
    for (const status of ADMIN_DOCUMENT_STATUSES) {
      expect(canReplaceKnowledgeDocumentContent(status)).toBe(true);
    }
  });

  it("builds process deactivate and reactivate confirmations", () => {
    expect(knowledgeDocumentProcessConfirmation({ title: "Horarios" }).body).toContain(
      "Horarios",
    );
    expect(
      knowledgeDocumentDeactivateConfirmation({ title: "Horarios" }).body,
    ).toContain("Horarios");
    expect(
      knowledgeDocumentReactivateConfirmation({ title: "Horarios" }).body,
    ).toContain("Horarios");
    expect(knowledgeDocumentReplaceContentWarning()).toMatch(/Recibido/);
  });

  it("builds knowledge list create and detail hrefs", () => {
    expect(adminKnowledgeHref()).toBe("/admin/knowledge");
    expect(adminKnowledgeHref({ query: "  " })).toBe("/admin/knowledge");
    expect(adminKnowledgeHref({ query: "horario" })).toBe(
      "/admin/knowledge?query=horario",
    );
    expect(adminKnowledgeHref({ query: "horario", limit: 5 })).toBe(
      "/admin/knowledge?query=horario&limit=5",
    );
    expect(adminKnowledgeHref({ query: "horario", limit: 10 })).toBe(
      "/admin/knowledge?query=horario",
    );
    expect(adminKnowledgeNewHref()).toBe("/admin/knowledge/new");
    expect(
      adminKnowledgeDocumentHref("dddddddd-dddd-dddd-dddd-dddddddddddd"),
    ).toBe("/admin/knowledge/dddddddd-dddd-dddd-dddd-dddddddddddd");
    expect(adminKnowledgeDocumentHref("id with spaces")).toBe(
      "/admin/knowledge/id%20with%20spaces",
    );
  });

  it("parses knowledge search query and limit", () => {
    expect(shouldSearchAdminKnowledge("")).toBe(false);
    expect(shouldSearchAdminKnowledge("  ")).toBe(false);
    expect(shouldSearchAdminKnowledge("horario")).toBe(true);
    expect(parseAdminKnowledgeSearchLimit(null)).toBe(10);
    expect(parseAdminKnowledgeSearchLimit("0")).toBe(10);
    expect(parseAdminKnowledgeSearchLimit("21")).toBe(10);
    expect(parseAdminKnowledgeSearchLimit("abc")).toBe(10);
    expect(parseAdminKnowledgeSearchLimit("7")).toBe(7);
    expect(
      adminKnowledgeSearchQueryFromSearchParams({
        query: "  horario  ",
        limit: "5",
      }),
    ).toEqual({ query: "horario", limit: 5 });
  });

  it("maps document status tones for badges", () => {
    expect(documentStatusTone("READY")).toBe("primary");
    expect(documentStatusTone("FAILED")).toBe("danger");
    expect(documentStatusTone("INACTIVE")).toBe("neutral");
    expect(documentStatusTone("RECEIVED")).toBe("accent");
    expect(documentStatusTone("CHUNKED")).toBe("accent");
  });

  it("builds payment detail hrefs", () => {
    expect(
      adminPaymentHref("55555555-5555-5555-5555-555555555555"),
    ).toBe("/admin/payments/55555555-5555-5555-5555-555555555555");
    expect(adminPaymentHref("id with spaces")).toBe(
      "/admin/payments/id%20with%20spaces",
    );
  });

  it("classifies payment detail errors", () => {
    expect(
      adminPaymentDetailErrorKind(
        new ApiError({ status: 404, code: "PAYMENT_NOT_FOUND" }),
      ),
    ).toBe("payment_not_found");
    expect(
      adminPaymentDetailErrorKind(
        new ApiError({ status: 500, code: "INTERNAL_ERROR" }),
      ),
    ).toBe("error");
    expect(adminPaymentDetailErrorKind(new Error("boom"))).toBe("error");
  });

  it("maps payment status tones without inventing REFUNDED", () => {
    expect(paymentStatusTone("APPROVED")).toBe("primary");
    expect(paymentStatusTone("PENDING")).toBe("accent");
    expect(paymentStatusTone("DECLINED")).toBe("danger");
  });

  it("labels the knowledge nav as Fercho", () => {
    const knowledge = ADMIN_NAV_LINKS.find(
      (link) => link.href === "/admin/knowledge",
    );
    expect(knowledge?.label).toBe("Fercho");
  });
});
