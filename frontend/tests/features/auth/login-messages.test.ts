import { describe, expect, it } from "vitest";
import { messageForApiProblem } from "@/shared/errors/messages";

describe("login credential messages", () => {
  it("maps INVALID_CREDENTIALS without revealing account existence", () => {
    expect(
      messageForApiProblem({
        status: 401,
        code: "INVALID_CREDENTIALS",
        detail: "Invalid credentials",
      }),
    ).toBe("Correo o contraseña incorrectos.");
  });
});
