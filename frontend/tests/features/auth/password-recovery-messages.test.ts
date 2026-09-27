import { describe, expect, it } from "vitest";
import { messageForApiProblem } from "@/shared/errors/messages";

describe("password recovery api messages", () => {
  it("maps invalid recovery tokens and rate limits", () => {
    expect(
      messageForApiProblem({
        status: 400,
        title: "Bad Request",
        detail: "x",
        code: "INVALID_PASSWORD_RECOVERY",
      }),
    ).toBe("El enlace de recuperación no es válido o ya expiró.");
    expect(
      messageForApiProblem({
        status: 429,
        title: "Too Many Requests",
        detail: "x",
        code: "PASSWORD_RECOVERY_RATE_LIMITED",
      }),
    ).toBe("Has realizado demasiadas solicitudes. Inténtalo más tarde.");
  });
});
