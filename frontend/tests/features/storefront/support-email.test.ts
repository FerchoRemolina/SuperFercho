import { describe, expect, it } from "vitest";
import {
  mailtoHref,
  normalizeSupportEmail,
} from "@/features/storefront/support-email";

describe("normalizeSupportEmail", () => {
  it("returns a trimmed plain email", () => {
    expect(normalizeSupportEmail("  SoporteSuperFercho@gmail.com ")).toBe(
      "SoporteSuperFercho@gmail.com",
    );
  });

  it("strips a mailto: prefix if present", () => {
    expect(
      normalizeSupportEmail("mailto:SoporteSuperFercho@gmail.com"),
    ).toBe("SoporteSuperFercho@gmail.com");
  });

  it("rejects empty or invalid values instead of building a bad link", () => {
    expect(normalizeSupportEmail("")).toBeNull();
    expect(normalizeSupportEmail("https://example.com")).toBeNull();
    expect(normalizeSupportEmail("not-an-email")).toBeNull();
  });
});

describe("mailtoHref", () => {
  it("builds a mailto: URL without https://", () => {
    expect(mailtoHref("SoporteSuperFercho@gmail.com")).toBe(
      "mailto:SoporteSuperFercho@gmail.com",
    );
  });
});
