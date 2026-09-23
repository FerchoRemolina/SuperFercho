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
    expect(listItem).toContain("useAddCartItemMutation");
    expect(listItem).toContain("quantity: item.quantity");
    expect(api).not.toContain("from-list");
    expect(api).not.toContain("/cart");
  });

  it("keeps list pages on customer routes without placeholders", () => {
    const indexPage = source("app/(customer)/lists/page.tsx");
    const detailPage = source(
      "app/(customer)/lists/[shoppingListId]/page.tsx",
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
});
