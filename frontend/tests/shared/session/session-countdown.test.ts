import { describe, expect, it } from "vitest";
import {
  formatSessionRemaining,
  sessionCountdownTone,
} from "@/shared/session/session-countdown";

describe("session countdown tone (tiempo real restante)", () => {
  it("green above 5 minutes: 05:01", () => {
    expect(sessionCountdownTone(5 * 60_000 + 1_000)).toBe("green");
  });

  it("yellow at exactly 05:00 and 02:00 (both inclusive)", () => {
    expect(sessionCountdownTone(5 * 60_000)).toBe("yellow");
    expect(sessionCountdownTone(2 * 60_000)).toBe("yellow");
  });

  it("red below 2 minutes: 01:59", () => {
    expect(sessionCountdownTone(2 * 60_000 - 1_000)).toBe("red");
    expect(sessionCountdownTone(0)).toBe("red");
  });

  it("is consistent with the displayed MM:SS value", () => {
    // 5:00 mostrado → amarillo (el cruce verde ocurre al pasar a 5:01).
    expect(sessionCountdownTone(5 * 60_000 + 1)).toBe("yellow");
    expect(sessionCountdownTone(5 * 60_000 + 59_000)).toBe("green");
  });
});

describe("formatSessionRemaining (formato existente, sin cambios)", () => {
  it("keeps MM:SS and the under-a-minute message", () => {
    expect(formatSessionRemaining(17 * 60_000 + 32_000)).toBe("17:32");
    expect(formatSessionRemaining(59_000)).toBe("Menos de 1 min");
  });
});
