import { describe, expect, it } from "vitest";
import {
  adminKnowledgeDocumentHref,
  adminKnowledgeHref,
  adminKnowledgeNewHref,
  adminOrderDetailErrorKind,
  adminOrderDetailHref,
  adminOrdersHref,
  adminOrdersListQueryFromSearchParams,
  adminOrdersPageCount,
  adminOrderStatusAdvanceConfirmation,
  adminOrderStatusAdvanceLabel,
  adminOrderStatusPanelState,
  adminProductsHref,
  canAdvanceAdminOrderStatus,
  canDeactivateKnowledgeDocument,
  canGoToNextAdminOrdersPage,
  canGoToPreviousAdminOrdersPage,
  canProcessKnowledgeDocument,
  canReactivateKnowledgeDocument,
  canReplaceKnowledgeDocumentContent,
  documentStatusLabel,
  documentStatusTone,
  formatAdminInstant,
  isAdminOrderStatusSubmitLocked,
  isAdminRole,
  knowledgeDocumentDeactivateConfirmation,
  knowledgeDocumentProcessConfirmation,
  knowledgeDocumentReactivateConfirmation,
  knowledgeDocumentReplaceContentWarning,
  listQueryFromSearchParams,
  nextAdminOrderStatus,
  parseAdminOrderStatus,
  parseAdminOrdersPage,
  shouldSearchAdminProducts,
} from "@/features/admin/presentation";
import { ADMIN_DOCUMENT_STATUSES } from "@/features/admin/api";
import { ApiError } from "@/shared/errors/api-problem";
import { formatMoney } from "@/shared/money/money";
import { messageForApiProblem } from "@/shared/errors/messages";

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
    expect(listQueryFromSearchParams({ status: "UNKNOWN" })).toEqual({});
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

  it("builds orders href with page and status filters", () => {
    expect(adminOrdersHref({})).toBe("/admin/orders");
    expect(adminOrdersHref({ page: 0, status: "" })).toBe("/admin/orders");
    expect(adminOrdersHref({ page: 2, status: "READY" })).toBe(
      "/admin/orders?page=2&status=READY",
    );
    expect(adminOrdersHref({ page: 0, status: "CANCELLED" })).toBe(
      "/admin/orders?status=CANCELLED",
    );
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

describe("admin order status advance", () => {
  it("maps only the linear ADMIN transitions", () => {
    expect(nextAdminOrderStatus("PENDING")).toBe("CONFIRMED");
    expect(nextAdminOrderStatus("CONFIRMED")).toBe("PREPARING");
    expect(nextAdminOrderStatus("PREPARING")).toBe("READY");
    expect(nextAdminOrderStatus("READY")).toBe("DELIVERED");
    expect(nextAdminOrderStatus("DELIVERED")).toBeNull();
    expect(nextAdminOrderStatus("CANCELLED")).toBeNull();
  });

  it("exposes advance labels only when an action exists", () => {
    expect(adminOrderStatusAdvanceLabel("PENDING")).toBe("Confirmar pedido");
    expect(adminOrderStatusAdvanceLabel("CONFIRMED")).toBe(
      "Pasar a preparación",
    );
    expect(adminOrderStatusAdvanceLabel("PREPARING")).toBe("Marcar como listo");
    expect(adminOrderStatusAdvanceLabel("READY")).toBe(
      "Marcar como entregado",
    );
    expect(adminOrderStatusAdvanceLabel("DELIVERED")).toBeNull();
    expect(adminOrderStatusAdvanceLabel("CANCELLED")).toBeNull();
    expect(canAdvanceAdminOrderStatus("PENDING")).toBe(true);
    expect(canAdvanceAdminOrderStatus("DELIVERED")).toBe(false);
    expect(canAdvanceAdminOrderStatus("CANCELLED")).toBe(false);
  });

  it("requires confirmation before submit and locks while pending", () => {
    expect(
      adminOrderStatusPanelState({
        status: "PENDING",
        confirming: false,
        isPending: false,
      }),
    ).toBe("idle");
    expect(
      adminOrderStatusPanelState({
        status: "PENDING",
        confirming: true,
        isPending: false,
      }),
    ).toBe("confirming");
    expect(
      adminOrderStatusPanelState({
        status: "PENDING",
        confirming: true,
        isPending: true,
      }),
    ).toBe("pending");
    expect(
      adminOrderStatusPanelState({
        status: "DELIVERED",
        confirming: false,
        isPending: false,
      }),
    ).toBe("hidden");
    expect(
      adminOrderStatusPanelState({
        status: "CANCELLED",
        confirming: true,
        isPending: false,
      }),
    ).toBe("hidden");
    expect(isAdminOrderStatusSubmitLocked(true)).toBe(true);
    expect(isAdminOrderStatusSubmitLocked(false)).toBe(false);
  });

  it("builds confirmation copy with order number and statuses", () => {
    const copy = adminOrderStatusAdvanceConfirmation({
      orderNumber: "ORD-P-1001",
      currentStatus: "PENDING",
      nextStatus: "CONFIRMED",
    });
    expect(copy.title).toMatch(/estado/i);
    expect(copy.body).toContain("ORD-P-1001");
    expect(copy.body).toContain("Pendiente");
    expect(copy.body).toContain("Confirmado");
  });

  it("maps status-update problem codes without cancel-oriented wording", () => {
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
        detail: "Invalid order state transition: PENDING -> PREPARING",
      }),
    ).toBe("El pedido ya no está en un estado que permita esta acción.");
    expect(
      messageForApiProblem({
        status: 400,
        code: "INVALID_ORDER_STATUS_UPDATE",
        detail: "Order status cannot be updated to: CANCELLED",
      }),
    ).toBe("No se puede establecer ese estado desde esta acción.");
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
    expect(adminKnowledgeNewHref()).toBe("/admin/knowledge/new");
    expect(
      adminKnowledgeDocumentHref("dddddddd-dddd-dddd-dddd-dddddddddddd"),
    ).toBe("/admin/knowledge/dddddddd-dddd-dddd-dddd-dddddddddddd");
    expect(adminKnowledgeDocumentHref("id with spaces")).toBe(
      "/admin/knowledge/id%20with%20spaces",
    );
  });

  it("maps document status tones for badges", () => {
    expect(documentStatusTone("READY")).toBe("primary");
    expect(documentStatusTone("FAILED")).toBe("danger");
    expect(documentStatusTone("INACTIVE")).toBe("neutral");
    expect(documentStatusTone("RECEIVED")).toBe("accent");
    expect(documentStatusTone("CHUNKED")).toBe("accent");
  });
});
