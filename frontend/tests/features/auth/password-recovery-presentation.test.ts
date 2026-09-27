import { describe, expect, it } from "vitest";
import {
  FORGOT_PASSWORD_PATH,
  forgotPasswordEmailError,
  GENERIC_RECOVERY_MESSAGE,
  isMissingRecoveryToken,
  isResetPasswordReady,
  resetConfirmPasswordError,
  resetPasswordFormErrors,
  resetPasswordPathWithToken,
  shouldShowPasswordRequirements,
} from "@/features/auth/password-recovery-presentation";

describe("password recovery presentation", () => {
  it("exposes the forgot-password path used from login", () => {
    expect(FORGOT_PASSWORD_PATH).toBe("/forgot-password");
  });

  it("validates recovery email like registration", () => {
    expect(forgotPasswordEmailError("")).toBe("El correo no puede quedar vacío.");
    expect(forgotPasswordEmailError("bad")).toBe(
      "Ingresa un correo electrónico válido.",
    );
    expect(forgotPasswordEmailError("ada@identity.test")).toBeUndefined();
  });

  it("keeps password requirements hidden until interaction", () => {
    expect(
      shouldShowPasswordRequirements({
        interacted: false,
        submitAttempted: false,
        passwordLength: 0,
      }),
    ).toBe(false);
    expect(
      shouldShowPasswordRequirements({
        interacted: true,
        submitAttempted: false,
        passwordLength: 0,
      }),
    ).toBe(true);
    expect(
      shouldShowPasswordRequirements({
        interacted: false,
        submitAttempted: true,
        passwordLength: 0,
      }),
    ).toBe(true);
    expect(
      shouldShowPasswordRequirements({
        interacted: false,
        submitAttempted: false,
        passwordLength: 1,
      }),
    ).toBe(true);
  });

  it("validates new password and confirmation", () => {
    expect(resetPasswordFormErrors({ password: "short", confirmPassword: "" }))
      .toEqual({
        password: "La contraseña no cumple los requisitos indicados.",
        confirmPassword: "Confirma tu nueva contraseña.",
      });
    expect(
      resetConfirmPasswordError("Luis123!", "Luis123!!"),
    ).toBe("Las contraseñas no coinciden.");
    expect(
      resetPasswordFormErrors({
        password: "Luis123!",
        confirmPassword: "Luis123!",
      }),
    ).toEqual({});
    expect(
      isResetPasswordReady({
        password: "Luis123!",
        confirmPassword: "Luis123!",
      }),
    ).toBe(true);
  });

  it("treats missing tokens as invalid recovery links", () => {
    expect(isMissingRecoveryToken(null)).toBe(true);
    expect(isMissingRecoveryToken("")).toBe(true);
    expect(isMissingRecoveryToken("  ")).toBe(true);
    expect(isMissingRecoveryToken("abc")).toBe(false);
    expect(resetPasswordPathWithToken("tok+en")).toBe(
      "/reset-password?token=tok%2Ben",
    );
  });

  it("keeps the generic recovery copy for success screens", () => {
    expect(GENERIC_RECOVERY_MESSAGE).toContain(
      "Si existe una cuenta asociada a este correo",
    );
  });
});
