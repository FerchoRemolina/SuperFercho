import { describe, expect, it } from "vitest";
import {
  ADMIN_CUSTOMER_DOCUMENT_TYPES,
  activeAdminCustomersPerspective,
  adminCustomerAccountStatus,
  adminCustomerAccountStatusLabel,
  adminCustomerAccountsSummary,
  adminCustomerAccountsSummaryLabel,
  adminCustomerDetailHref,
  adminCustomerDocumentTypeLabel,
  adminCustomerOrderStatusToneClass,
  adminCustomerPageRangeLabel,
  adminCustomerPaymentStatusLabel,
  adminCustomerRecordStatusLabel,
  adminCustomersHref,
  adminCustomersListQueryFromSearchParams,
  adminCustomersPerspectiveParams,
  adminCustomersSortFromSelectValue,
  adminCustomersSortSelectValue,
  canGoToNextAdminCustomersPage,
  canGoToPreviousAdminCustomersPage,
  canSearchAdminCustomer,
  isValidAdminCustomerDocumentNumber,
  resolveAdminCustomersPerspective,
} from "@/features/admin/customers-presentation";

describe("búsqueda de clientes por documento", () => {
  it("solo acepta los tipos de documento de Identity (CC/CE)", () => {
    expect(ADMIN_CUSTOMER_DOCUMENT_TYPES.map((t) => t.value)).toEqual([
      "CC",
      "CE",
    ]);
    expect(canSearchAdminCustomer("TI", "123456789")).toBe(false);
  });

  it("el número debe ser numérico de hasta 10 dígitos", () => {
    expect(isValidAdminCustomerDocumentNumber("1234567890")).toBe(true);
    expect(isValidAdminCustomerDocumentNumber("12a456")).toBe(false);
    expect(isValidAdminCustomerDocumentNumber("12345678901")).toBe(false);
  });

  it("búsqueda válida con CC y número completo", () => {
    expect(canSearchAdminCustomer("CC", "123456789")).toBe(true);
  });

  it("búsqueda incompleta (sin número) no se ejecuta", () => {
    expect(canSearchAdminCustomer("CC", "")).toBe(false);
    expect(canSearchAdminCustomer("CC", "   ")).toBe(false);
  });
});

describe("estado de cuentas (ACTIVE/INACTIVE/DELETED)", () => {
  it("deletedAt define el estado DELETED aunque el status sea ACTIVE", () => {
    expect(
      adminCustomerAccountStatus({ status: "ACTIVE", deletedAt: null }),
    ).toBe("ACTIVE");
    expect(
      adminCustomerAccountStatus({
        status: "ACTIVE",
        deletedAt: "2026-01-01T00:00:00Z",
      }),
    ).toBe("DELETED");
    expect(
      adminCustomerAccountStatus({ status: "INACTIVE", deletedAt: null }),
    ).toBe("INACTIVE");
  });

  it("labels en español para cada estado", () => {
    expect(adminCustomerAccountStatusLabel("ACTIVE")).toBe("Activa");
    expect(adminCustomerAccountStatusLabel("INACTIVE")).toBe("Inactiva");
    expect(adminCustomerAccountStatusLabel("DELETED")).toBe("Eliminada");
  });
});

describe("resumen de cuentas", () => {
  it("cuenta activas, inactivas y eliminadas", () => {
    const summary = adminCustomerAccountsSummary([
      { status: "ACTIVE", deletedAt: null },
      { status: "ACTIVE", deletedAt: null },
      { status: "INACTIVE", deletedAt: null },
      { status: "ACTIVE", deletedAt: "2026-01-01T00:00:00Z" },
    ]);
    expect(summary).toEqual({
      total: 4,
      active: 2,
      inactive: 1,
      deleted: 1,
    });
  });

  it("el label omite los conteos en cero y usa singular/plural", () => {
    expect(
      adminCustomerAccountsSummaryLabel(
        adminCustomerAccountsSummary([
          { status: "ACTIVE", deletedAt: null },
          { status: "ACTIVE", deletedAt: null },
          { status: "INACTIVE", deletedAt: null },
          { status: "ACTIVE", deletedAt: "2026-01-01T00:00:00Z" },
        ]),
      ),
    ).toBe("2 activas · 1 inactiva · 1 eliminada");
    expect(
      adminCustomerAccountsSummaryLabel(
        adminCustomerAccountsSummary([
          { status: "ACTIVE", deletedAt: null },
          { status: "INACTIVE", deletedAt: null },
        ]),
      ),
    ).toBe("1 activa · 1 inactiva");
  });

  it("sin cuentas devuelve el label vacío controlado", () => {
    expect(
      adminCustomerAccountsSummaryLabel(
        adminCustomerAccountsSummary([]),
      ),
    ).toBe("Sin cuentas");
  });
});

describe("presentación de pedidos y pagos", () => {
  it("tonos de estado de pedido por semántica", () => {
    expect(adminCustomerOrderStatusToneClass("DELIVERED")).toContain(
      "emerald",
    );
    expect(adminCustomerOrderStatusToneClass("CANCELLED")).toContain("red");
    expect(adminCustomerOrderStatusToneClass("PREPARING")).toContain("amber");
  });

  it("labels de estado de pago", () => {
    expect(adminCustomerPaymentStatusLabel("APPROVED")).toBe("Aprobado");
    expect(adminCustomerPaymentStatusLabel("PENDING")).toBe("Pendiente");
    expect(adminCustomerPaymentStatusLabel("DECLINED")).toBe("Rechazado");
  });
});

describe("paginación de historiales", () => {
  it("rango de la primera página", () => {
    expect(
      adminCustomerPageRangeLabel(0, 10, 23, "pedido", "pedidos"),
    ).toBe("Mostrando 1 a 10 de 23 pedidos");
  });

  it("última página no se pasa del total", () => {
    expect(
      adminCustomerPageRangeLabel(2, 10, 23, "pedido", "pedidos"),
    ).toBe("Mostrando 21 a 23 de 23 pedidos");
  });

  it("un solo elemento usa singular", () => {
    expect(
      adminCustomerPageRangeLabel(0, 10, 1, "pago", "pagos"),
    ).toBe("Mostrando 1 a 1 de 1 pago");
  });
});

describe("labels de tipo de documento", () => {
  it("CC y CE tienen etiquetas legibles", () => {
    expect(adminCustomerDocumentTypeLabel("CC")).toBe(
      "Cédula de ciudadanía (CC)",
    );
    expect(adminCustomerDocumentTypeLabel("CE")).toBe(
      "Cédula de extranjería (CE)",
    );
  });
});

describe("listado admin de clientes", () => {
  it("parsea query params del listado con defaults del backend", () => {
    expect(adminCustomersListQueryFromSearchParams({})).toEqual({
      search: undefined,
      status: "ALL",
      sortBy: "CREATED_AT",
      sortDir: "DESC",
      hasPurchases: undefined,
      page: 0,
      size: 20,
    });
    expect(
      adminCustomersListQueryFromSearchParams({
        search: " ana ",
        status: "ACTIVE",
        sortBy: "TOTAL_SPENT",
        sortDir: "DESC",
        hasPurchases: "false",
        page: "2",
      }),
    ).toEqual({
      search: "ana",
      status: "ACTIVE",
      sortBy: "TOTAL_SPENT",
      sortDir: "DESC",
      hasPurchases: false,
      page: 2,
      size: 20,
    });
    expect(
      adminCustomersListQueryFromSearchParams({ status: "CLOSED" }).status,
    ).toBe("ALL");
    expect(
      adminCustomersListQueryFromSearchParams({ status: "NO_ACCOUNT" }).status,
    ).toBe("ALL");
    expect(
      adminCustomersListQueryFromSearchParams({ status: "DELETED" }).status,
    ).toBe("ALL");
  });

  it("acepta hasPurchases true/false en variantes del contrato", () => {
    expect(
      adminCustomersListQueryFromSearchParams({ hasPurchases: "yes" })
        .hasPurchases,
    ).toBe(true);
    expect(
      adminCustomersListQueryFromSearchParams({ hasPurchases: "0" })
        .hasPurchases,
    ).toBe(false);
  });

  it("traduce opciones de ordenamiento al contrato REST", () => {
    expect(adminCustomersSortSelectValue("ORDERS", "DESC")).toBe(
      "ORDERS:DESC",
    );
    expect(adminCustomersSortFromSelectValue("TOTAL_SPENT:DESC")).toEqual({
      sortBy: "TOTAL_SPENT",
      sortDir: "DESC",
    });
    expect(adminCustomersSortFromSelectValue("NAME:ASC")).toEqual({
      sortBy: "NAME",
      sortDir: "ASC",
    });
  });

  it("perspectivas rápidas mapean a params del mismo listado", () => {
    expect(adminCustomersPerspectiveParams("all")).toEqual({
      sortBy: "CREATED_AT",
      sortDir: "DESC",
      hasPurchases: undefined,
    });
    expect(adminCustomersPerspectiveParams("recent")).toEqual({
      sortBy: "CREATED_AT",
      sortDir: "DESC",
      hasPurchases: undefined,
    });
    expect(adminCustomersPerspectiveParams("activity")).toEqual({
      sortBy: "ORDERS",
      sortDir: "DESC",
      hasPurchases: undefined,
    });
    expect(adminCustomersPerspectiveParams("spend")).toEqual({
      sortBy: "TOTAL_SPENT",
      sortDir: "DESC",
      hasPurchases: undefined,
    });
    expect(adminCustomersPerspectiveParams("no_purchases")).toEqual({
      sortBy: "CREATED_AT",
      sortDir: "DESC",
      hasPurchases: false,
    });
  });

  it("infiere la perspectiva activa sin inventar rutas", () => {
    expect(
      resolveAdminCustomersPerspective({
        sortBy: "CREATED_AT",
        sortDir: "DESC",
      }),
    ).toBe("all");
    expect(
      resolveAdminCustomersPerspective({
        sortBy: "ORDERS",
        sortDir: "DESC",
      }),
    ).toBe("activity");
    expect(
      resolveAdminCustomersPerspective({
        sortBy: "TOTAL_SPENT",
        sortDir: "DESC",
      }),
    ).toBe("spend");
    expect(
      resolveAdminCustomersPerspective({
        sortBy: "CREATED_AT",
        sortDir: "DESC",
        hasPurchases: false,
      }),
    ).toBe("no_purchases");
    expect(
      activeAdminCustomersPerspective(
        { sortBy: "CREATED_AT", sortDir: "DESC" },
        "recent",
      ),
    ).toBe("recent");
  });

  it("arma href del listado omitiendo defaults", () => {
    expect(adminCustomersHref({})).toBe("/admin/customers");
    expect(
      adminCustomersHref({
        search: "ana",
        status: "ACTIVE",
        sortBy: "ORDERS",
        sortDir: "DESC",
        hasPurchases: false,
        page: 1,
      }),
    ).toBe(
      "/admin/customers?search=ana&status=ACTIVE&sortBy=ORDERS&sortDir=DESC&hasPurchases=false&page=1",
    );
    expect(adminCustomersHref({ perspective: "recent" })).toBe(
      "/admin/customers?perspective=recent",
    );
  });

  it("href de detalle usa el id del CustomerRecord", () => {
    expect(
      adminCustomerDetailHref("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ).toBe("/admin/customers/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  });

  it("labels de estado del record (no de cuenta)", () => {
    expect(adminCustomerRecordStatusLabel("ACTIVE")).toBe("Activo");
    expect(adminCustomerRecordStatusLabel("INACTIVE")).toBe("Inactivo");
  });

  it("paginación del listado", () => {
    expect(canGoToPreviousAdminCustomersPage(0)).toBe(false);
    expect(canGoToPreviousAdminCustomersPage(1)).toBe(true);
    expect(canGoToNextAdminCustomersPage(0, 20, 20)).toBe(false);
    expect(canGoToNextAdminCustomersPage(0, 20, 21)).toBe(true);
  });
});
