import { describe, expect, it } from "vitest";
import { safeNextPath } from "@/shared/auth/safe-next-path";

describe("safeNextPath", () => {
  it("keeps internal paths", () => {
    expect(safeNextPath("/cart")).toBe("/cart");
    expect(safeNextPath("/products/abc")).toBe("/products/abc");
  });

  it("rejects external or protocol-relative values", () => {
    expect(safeNextPath("https://evil.test")).toBeNull();
    expect(safeNextPath("//evil.test")).toBeNull();
    expect(safeNextPath("/\\evil.test")).toBeNull();
    expect(safeNextPath(null)).toBeNull();
  });
});
