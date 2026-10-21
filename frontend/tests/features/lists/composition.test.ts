import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { shoppingListsKeys } from "@/features/lists/api";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("shopping lists composition", () => {
  it("keeps stable query keys for list and detail", () => {
    expect(shoppingListsKeys().root()).toEqual(["shopping-lists"]);
    expect(shoppingListsKeys().detail("list-1")).toEqual([
      "shopping-lists",
      "detail",
      "list-1",
    ]);
  });

  it("wires catalog surfaces to AddToListControl without inventing list-to-cart REST", () => {
    const productCard = source("features/catalog/components/product-card.tsx");
    const productDetail = source(
      "features/catalog/components/product-detail-content.tsx",
    );
    const addControl = source(
      "features/lists/components/add-to-list-control.tsx",
    );
    const listItem = source("features/lists/components/list-item-card.tsx");
    const api = source("features/lists/api.ts");

    expect(productCard).toContain("AddToListControl");
    expect(productDetail).toContain("AddToListControl");
    expect(addControl).toContain("useAddShoppingListItemMutation");
    expect(addControl).not.toContain("/api/v1/shopping-lists/");
    expect(addControl).not.toContain("from-list");
    expect(addControl).not.toContain("Gestionar listas");
    expect(addControl).not.toContain("Crear lista y agregar");
    expect(addControl).toContain("lists.length === 0");
    // Pertenencia idempotente: marca listas que ya contienen el producto y
    // consume alreadyPresent del backend sin tratarlo como error.
    expect(addControl).toContain("shoppingListContainsProduct");
    expect(addControl).toContain("alreadyPresent");
    expect(addControl).toContain("Ya está en esta lista");
    expect(addControl).toContain("El producto ya está en esta lista.");
    // La lista no es un carrito: el ítem no agrega al carrito por su cuenta.
    expect(listItem).not.toContain("useAddCartItemMutation");
    expect(listItem).not.toContain("Agregar al carrito");
    expect(listItem).toContain("ListQuantityStepper");
    expect(listItem).toContain("Eliminar");
    // Compra de la lista: un único endpoint oficial, sin loop por producto.
    expect(api).toContain("/add-to-cart");
    expect(api).not.toContain("/cart/items");
    expect(api).toContain("shoppingListPurchaseSummary");
  });

  it("gates lists intro and empty-state copy for zero lists", () => {
    const listsPage = source(
      "features/lists/components/lists-page-content.tsx",
    );
    expect(listsPage).toContain("{!hasLists ? (");
    expect(listsPage).toContain("Guarda productos y arma tu mercado a tu ritmo.");
    expect(listsPage).toContain("cumpleaños");
    expect(listsPage).toContain("asados");
    expect(listsPage).toContain("ocasiones especiales");
    expect(listsPage).toContain("mercado");
  });

  it("supports delete list with inline confirmation and redirect", () => {
    const detail = source(
      "features/lists/components/list-detail-page-content.tsx",
    );
    const hooks = source("features/lists/hooks.ts");
    const api = source("features/lists/api.ts");

    expect(api).toContain("deleteShoppingList");
    expect(hooks).toContain("useDeleteShoppingListMutation");
    expect(detail).toContain("Eliminar lista");
    expect(detail).toContain("¿Eliminar esta lista?");
    expect(detail).toContain("confirmingDelete");
    expect(detail).toContain('router.push("/lists")');
    expect(detail).toContain("useDeleteShoppingListMutation");
  });

  it("keeps list pages on customer routes without placeholders", () => {
    const indexPage = source("app/(storefront)/(customer)/lists/page.tsx");
    const detailPage = source(
      "app/(storefront)/(customer)/lists/[shoppingListId]/page.tsx",
    );
    expect(indexPage).toContain("ListsPageContent");
    expect(indexPage).not.toContain("PlaceholderScreen");
    expect(detailPage).toContain("ListDetailPageContent");
    expect(detailPage).not.toContain("PlaceholderScreen");
  });

  it("never sends customerId from list forms or API wrappers", () => {
    const api = source("features/lists/api.ts");
    const createForm = source(
      "features/lists/components/create-list-form.tsx",
    );
    expect(api).toContain("never sent by the client");
    expect(createForm).not.toContain("customerId");
    expect(api).not.toMatch(/body:.*customerId/);
  });

  it("buys the list with one global action against the official endpoint", () => {
    const detail = source(
      "features/lists/components/list-detail-page-content.tsx",
    );
    const hooks = source("features/lists/hooks.ts");

    expect(detail).toContain("useAddShoppingListToCartMutation");
    expect(detail).not.toContain("useAddCartItemMutation");
    expect(detail).toContain("Comprar esta lista");
    expect(detail).toContain('router.push("/cart")');
    expect(detail).toContain("shoppingListPurchaseSummary");
    expect(detail).toContain("buyMutation.isPending");
    expect(hooks).toContain("useAddShoppingListToCartMutation");
    expect(hooks).toContain("cartKeys");
  });

  it("keeps quantity management and explicit removal separate from buying", () => {
    const stepper = source("features/lists/components/list-quantity-stepper.tsx");
    const listItem = source("features/lists/components/list-item-card.tsx");

    // Mínimo 0: el producto permanece en la lista sin eliminarse.
    expect(stepper).toContain("next < 0");
    expect(stepper).toContain("quantity <= 0");
    expect(listItem).toContain("item.quantity === 0");
    expect(listItem).toContain("Guardado · no se compra ahora");
    expect(listItem).toContain("useRemoveShoppingListItemMutation");
  });

  it("hides Agregar al carrito when stock is 0 while keeping sellability separate", () => {
    const listItem = source("features/lists/components/list-item-card.tsx");
    expect(listItem).not.toContain("Agregar al carrito");
    expect(listItem).toContain("productAvailabilityLabel");
    expect(listItem).toContain("productStockLabel");
    expect(listItem).toContain("agotado");
    expect(listItem).toContain("No disponible");
  });
});
