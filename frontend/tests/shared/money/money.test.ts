import { describe, expect, it } from "vitest";
import { formatMoney } from "@/shared/money/money";

describe("formatMoney", () => {
  it("formats COP with two decimal places for es-CO", () => {
    const formatted = formatMoney({ amount: 10.5, currency: "COP" });
    expect(formatted).toContain("10,50");
    expect(formatted).toMatch(/COP|\$/);
  });
});
