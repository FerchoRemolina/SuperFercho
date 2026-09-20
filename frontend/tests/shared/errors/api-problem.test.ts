import { describe, expect, it } from "vitest";
import { parseApiProblem } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";

describe("parseApiProblem", () => {
  it("copies RFC 7807 fields and the custom code property", () => {
    expect(
      parseApiProblem(409, {
        type: "about:blank",
        title: "Conflict",
        status: 409,
        detail: "El precio cambió",
        instance: "/api/v1/orders",
        code: "PRODUCT_PRICE_CHANGED",
      }),
    ).toEqual({
      status: 409,
      code: "PRODUCT_PRICE_CHANGED",
      title: "Conflict",
      detail: "El precio cambió",
    });
  });

  it("falls back to the HTTP status when the body is not a problem object", () => {
    expect(parseApiProblem(500, "oops")).toEqual({ status: 500 });
  });
});

describe("messageForApiProblem", () => {
  it("prefers the backend detail when present", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "STOCK_UNAVAILABLE",
        detail: "No hay stock suficiente",
      }),
    ).toBe("No hay stock suficiente");
  });

  it("uses a generic internal message for 500 without leaking a stack", () => {
    expect(
      messageForApiProblem({
        status: 500,
        code: "INTERNAL_ERROR",
        title: "Internal Server Error",
      }),
    ).toBe("Ocurrió un error interno. Inténtalo más tarde.");
  });
});
