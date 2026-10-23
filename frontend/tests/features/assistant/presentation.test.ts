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
  packageOptionLabel,
  packageProposalTitle,
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

  it("labels package chips without reusing checkout copy", () => {
    const proposal = {
      proposalId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      productId: "dddddddd-dddd-dddd-dddd-dddddddddddd",
      productName: "Arroz",
      presentationQuantity: "1.000",
      presentationUnit: "KG",
      requestedQuantity: "2.300",
      requestedUnit: "KG",
      options: [
        { packageCount: 2, physicalTotalBase: "2000.000", baseUnit: "G" },
      ],
    };
    expect(packageProposalTitle(proposal)).toBe("Elige paquetes de Arroz");
    expect(packageOptionLabel(proposal.options[0])).toBe(
      "2 paquete(s) · 2000.000 G",
    );
    expect(packageProposalTitle(proposal)).not.toBe(confirmationTitle("CHECKOUT"));
    expect(packageOptionLabel(proposal.options[0])).not.toBe(
      confirmationConfirmLabel("CHECKOUT"),
    );
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
