import { describe, expect, it } from "vitest";
import { loginPathWithNext, safeNextPath } from "@/shared/auth/safe-next-path";

describe("safeNextPath", () => {
  it("keeps internal paths", () => {
    expect(safeNextPath("/cart")).toBe("/cart");
    expect(safeNextPath("/products/abc")).toBe("/products/abc");
    expect(safeNextPath("/search?text=leche")).toBe("/search?text=leche");
  });

  it("rejects external or protocol-relative values", () => {
    expect(safeNextPath("https://evil.test")).toBeNull();
    expect(safeNextPath("//evil.test")).toBeNull();
    expect(safeNextPath("/\\evil.test")).toBeNull();
    expect(safeNextPath(null)).toBeNull();
  });
});

describe("loginPathWithNext", () => {
  it("encodes the current path including the query string", () => {
    expect(loginPathWithNext("/products/abc")).toBe(
      "/login?next=%2Fproducts%2Fabc",
    );
    expect(loginPathWithNext("/catalog")).toBe("/login?next=%2Fcatalog");
    expect(loginPathWithNext("/search?text=leche")).toBe(
      "/login?next=%2Fsearch%3Ftext%3Dleche",
    );
  });
});
