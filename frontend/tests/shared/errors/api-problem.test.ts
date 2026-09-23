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
  it("prefers a Spanish status fallback when the code has no mapped message", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "UNKNOWN_CONFLICT",
        detail: "Detalle del backend",
      }),
    ).toBe("No se pudo completar la operación.");
  });

  it("never surfaces English backend detail for unmapped codes", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "UNKNOWN_CONFLICT",
        detail: "Product price changed: 33333333-3333-3333-3333-333333333333",
      }),
    ).toBe("No se pudo completar la operación.");
  });

  it("uses a Spanish message for STOCK_UNAVAILABLE even if the backend detail includes an id", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "STOCK_UNAVAILABLE",
        detail: "Stock unavailable for product: 33333333-3333-3333-3333-333333333333",
      }),
    ).toBe("No hay existencias suficientes. Revisa las cantidades.");
  });

  it("uses a Spanish message for PRODUCT_PRICE_CHANGED instead of the English detail", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "PRODUCT_PRICE_CHANGED",
        detail: "Product price changed: 33333333-3333-3333-3333-333333333333",
      }),
    ).toBe(
      "El precio de uno o más productos cambió. Revisa el pedido antes de confirmar.",
    );
  });

  it("uses a Spanish message for ADDRESS_NOT_AVAILABLE", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "ADDRESS_NOT_AVAILABLE",
        detail: "Address not available: 22222222-2222-2222-2222-222222222222",
      }),
    ).toBe("La dirección seleccionada ya no está disponible. Elige otra.");
  });

  it("uses a Spanish message for PRODUCT_NOT_FOUND even if the backend detail includes the id", () => {
    expect(
      messageForApiProblem({
        status: 404,
        code: "PRODUCT_NOT_FOUND",
        detail: "Product not found: 33333333-3333-3333-3333-333333333333",
      }),
    ).toBe("No encontramos este producto.");
  });

  it("uses a Spanish message for PRODUCT_STOCK_CONFLICT", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "PRODUCT_STOCK_CONFLICT",
        detail:
          "Product stock changed concurrently: 33333333-3333-3333-3333-333333333333",
      }),
    ).toBe(
      "El stock cambió mientras lo actualizabas. Revisa el valor actual e inténtalo de nuevo.",
    );
  });

  it("uses a Spanish message for INVALID_CREDENTIALS even if the backend detail is English", () => {
    expect(
      messageForApiProblem({
        status: 401,
        code: "INVALID_CREDENTIALS",
        detail: "Invalid credentials",
      }),
    ).toBe("Correo o contraseña incorrectos.");
  });

  it("uses a Spanish message for CART_NOT_FOUND even if the backend detail includes the customer id", () => {
    expect(
      messageForApiProblem({
        status: 404,
        code: "CART_NOT_FOUND",
        detail: "Cart not found for customer: 11111111-1111-1111-1111-111111111111",
      }),
    ).toBe("No encontramos tu carrito.");
  });

  it("uses a Spanish message for SHOPPING_LIST_NOT_FOUND", () => {
    expect(
      messageForApiProblem({
        status: 404,
        code: "SHOPPING_LIST_NOT_FOUND",
        detail: "Shopping list not found: bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      }),
    ).toBe("No encontramos esa lista.");
  });

  it("uses a Spanish message for INVALID_CART_ITEM instead of the English detail", () => {
    expect(
      messageForApiProblem({
        status: 400,
        code: "INVALID_CART_ITEM",
        detail: "quantity must be greater than 0",
      }),
    ).toBe("La cantidad debe ser mayor que cero.");
  });
  it("uses a Spanish message for PAYMENT_NOT_FOUND without leaking ids", () => {
    expect(
      messageForApiProblem({
        status: 404,
        code: "PAYMENT_NOT_FOUND",
        detail: "Payment not found: 55555555-5555-5555-5555-555555555555",
      }),
    ).toBe("No se pudo cargar la información de pago de este pedido.");
  });

  it("uses a Spanish message for CANCELLATION_NOT_ALLOWED", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "CANCELLATION_NOT_ALLOWED",
        detail: "customer cancellation window has expired",
      }),
    ).toBe(
      "Este pedido ya no puede cancelarse. El plazo de 15 minutos terminó o el pedido ya cambió de estado.",
    );
  });

  it("uses a neutral Spanish message for INVALID_ORDER_TRANSITION", () => {
    expect(
      messageForApiProblem({
        status: 409,
        code: "INVALID_ORDER_TRANSITION",
        detail: "Invalid order state transition: PENDING -> PREPARING",
      }),
    ).toBe("El pedido ya no está en un estado que permita esta acción.");
  });

  it("uses a Spanish message for INVALID_ORDER_STATUS_UPDATE", () => {
    expect(
      messageForApiProblem({
        status: 400,
        code: "INVALID_ORDER_STATUS_UPDATE",
        detail: "Order status cannot be updated to: CANCELLED",
      }),
    ).toBe("No se puede establecer ese estado desde esta acción.");
  });

  it("uses Spanish messages for Knowledge problem codes", () => {
    expect(
      messageForApiProblem({
        status: 400,
        code: "INVALID_DOCUMENT",
        detail: "document can only be deactivated when READY",
      }),
    ).toBe("El documento no es válido para esta operación.");
    expect(
      messageForApiProblem({
        status: 404,
        code: "DOCUMENT_NOT_FOUND",
        detail: "Document not found: dddddddd-dddd-dddd-dddd-dddddddddddd",
      }),
    ).toBe("No encontramos ese documento.");
    expect(
      messageForApiProblem({
        status: 409,
        code: "KNOWLEDGE_PROCESSING_FAILED",
        detail: "document processing failed",
      }),
    ).toBe("No se pudo procesar el documento. Inténtalo de nuevo.");
    expect(
      messageForApiProblem({
        status: 400,
        code: "INVALID_SEARCH_REQUEST",
        detail: "limit must be between 1 and 20",
      }),
    ).toBe(
      "La búsqueda no es válida. Revisa el límite (1 a 20) e inténtalo de nuevo.",
    );
  });

  it("uses a generic internal message for 500 without leaking a stack", () => {
    expect(
      messageForApiProblem({
        status: 500,
        code: "INTERNAL_ERROR",
        title: "Internal Server Error",
        detail: "java.lang.IllegalStateException",
      }),
    ).toBe("Ocurrió un error interno. Inténtalo más tarde.");
  });
});
