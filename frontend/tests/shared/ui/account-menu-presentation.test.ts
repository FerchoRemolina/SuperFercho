import { describe, expect, it } from "vitest";
import {
  accountAvatarInitial,
  accountMenuDisplayName,
  customerDisplayFullName,
} from "@/shared/ui/account-menu-presentation";

describe("account menu presentation", () => {
  it("derives the avatar initial from the first letter of the display name", () => {
    expect(accountAvatarInitial("Luis")).toBe("L");
    expect(accountAvatarInitial("maría")).toBe("M");
    expect(accountAvatarInitial("")).toBe("?");
  });

  it("uses the authenticated firstName and keeps preview label only for preview", () => {
    expect(
      accountMenuDisplayName({ firstName: "Carlos", isPreview: false }),
    ).toBe("Carlos");
    expect(
      accountMenuDisplayName({ firstName: "María", isPreview: false }),
    ).toBe("María");
    expect(
      accountMenuDisplayName({ firstName: undefined, isPreview: false }),
    ).toBeNull();
    expect(
      accountMenuDisplayName({ firstName: "Carlos", isPreview: true }),
    ).toBe("Cliente de prueba");
  });

  it("builds full name from first and last name", () => {
    expect(
      customerDisplayFullName({ firstName: "Luis", lastName: "Remolina" }),
    ).toBe("Luis Remolina");
    expect(
      customerDisplayFullName({ firstName: "Luis", lastName: undefined }),
    ).toBe("Luis");
  });
});
