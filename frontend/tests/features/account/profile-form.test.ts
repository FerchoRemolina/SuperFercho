import { describe, expect, it } from "vitest";
import { ApiError } from "@/shared/errors/api-problem";
import { profileServerFeedback } from "@/features/account/components/profile-form";

describe("profileServerFeedback", () => {
  it("maps duplicate document errors to the document field", () => {
    const feedback = profileServerFeedback(
      new ApiError({ status: 409, code: "DOCUMENT_ALREADY_EXISTS" }),
    );
    expect(feedback.fieldErrors.documentNumber).toBe(
      "Ya existe una cuenta con ese documento.",
    );
    expect(feedback.general).toBeNull();
  });

  it("maps duplicate email errors to the email field", () => {
    const feedback = profileServerFeedback(
      new ApiError({ status: 409, code: "USER_ALREADY_EXISTS" }),
    );
    expect(feedback.fieldErrors.email).toBe(
      "Ya existe una cuenta con ese correo.",
    );
    expect(feedback.general).toBeNull();
  });

  it("surfaces backend 400 validation failures as a clear general message", () => {
    const feedback = profileServerFeedback(
      new ApiError({
        status: 400,
        code: "INVALID_REGISTRATION",
        detail: "Ingresa un número de celular válido.",
      }),
    );
    expect(feedback.general).toBe(
      "Revisa los datos ingresados e inténtalo de nuevo.",
    );
    expect(feedback.fieldErrors).toEqual({});
  });

  it("surfaces forbidden profiles as a general message", () => {
    const feedback = profileServerFeedback(
      new ApiError({ status: 403, code: "CUSTOMER_PROFILE_FORBIDDEN" }),
    );
    expect(feedback.general).toBe("Tu cuenta no puede gestionar este perfil.");
    expect(feedback.fieldErrors).toEqual({});
  });

  it("falls back to a generic message for non API errors", () => {
    const feedback = profileServerFeedback(new Error("network down"));
    expect(feedback.general).toBe(
      "No se pudo actualizar el perfil. Inténtalo de nuevo.",
    );
    expect(feedback.fieldErrors).toEqual({});
  });
});
