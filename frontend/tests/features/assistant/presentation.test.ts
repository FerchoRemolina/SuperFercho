import { describe, expect, it } from "vitest";
import { addressKeys } from "@/features/account/api";
import { cartKeys } from "@/features/cart/api";
import { shoppingListsKeys } from "@/features/lists/api";
import { orderKeys } from "@/features/orders/api";
import {
  confirmationConfirmLabel,
  confirmationDescription,
  confirmationTitle,
  nextMessageId,
} from "@/features/assistant/presentation";

describe("assistant presentation", () => {
  it("labels CHECKOUT and CANCEL_ORDER confirmations in Spanish", () => {
    expect(confirmationTitle("CHECKOUT")).toBe("Confirmar pedido");
    expect(confirmationTitle("CANCEL_ORDER")).toBe("Confirmar cancelación");
    expect(confirmationConfirmLabel("CHECKOUT")).toBe("Confirmar pedido");
    expect(confirmationConfirmLabel("CANCEL_ORDER")).toBe(
      "Confirmar cancelación",
    );
    expect(confirmationDescription("CHECKOUT")).toContain("pedido");
    expect(confirmationDescription("CANCEL_ORDER")).toContain("cancelación");
  });

  it("builds stable local message ids", () => {
    expect(nextMessageId([], "user")).toBe("user-1");
    expect(
      nextMessageId(
        [{ id: "user-1", role: "user", content: "Hola" }],
        "assistant",
      ),
    ).toBe("assistant-2");
  });
});

describe("assistant query keys used for invalidation", () => {
  it("reuses existing customer query roots", () => {
    expect(cartKeys().root()).toEqual(["cart"]);
    expect(orderKeys().all).toEqual(["orders"]);
    expect(shoppingListsKeys().root()).toEqual(["shopping-lists"]);
    expect(addressKeys().root()).toEqual(["addresses"]);
  });
});
