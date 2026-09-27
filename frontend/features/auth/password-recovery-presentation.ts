import {
  isPasswordValid,
  validateEmail,
  validatePassword,
} from "@/features/auth/register-validation";

export const FORGOT_PASSWORD_PATH = "/forgot-password";
export const RESET_PASSWORD_PATH = "/reset-password";

export const GENERIC_RECOVERY_MESSAGE =
  "Si existe una cuenta asociada a este correo, recibirás un enlace para restablecer tu contraseña.";

export function forgotPasswordEmailError(email: string): string | undefined {
  return validateEmail(email) ?? undefined;
}

export function shouldShowPasswordRequirements(options: {
  interacted: boolean;
  submitAttempted: boolean;
  passwordLength: number;
}): boolean {
  return (
    options.interacted ||
    options.submitAttempted ||
    options.passwordLength > 0
  );
}

export function resetConfirmPasswordError(
  password: string,
  confirmPassword: string,
): string | undefined {
  if (!confirmPassword.trim()) {
    return "Confirma tu nueva contraseña.";
  }
  if (confirmPassword !== password) {
    return "Las contraseñas no coinciden.";
  }
  return undefined;
}

export function resetPasswordFormErrors(values: {
  password: string;
  confirmPassword: string;
}): { password?: string; confirmPassword?: string } {
  const password = validatePassword(values.password) ?? undefined;
  const confirmPassword = resetConfirmPasswordError(
    values.password,
    values.confirmPassword,
  );
  return {
    ...(password ? { password } : {}),
    ...(confirmPassword ? { confirmPassword } : {}),
  };
}

export function isResetPasswordReady(values: {
  password: string;
  confirmPassword: string;
}): boolean {
  return (
    isPasswordValid(values.password) &&
    values.confirmPassword === values.password
  );
}

export function resetPasswordPathWithToken(token: string): string {
  return `${RESET_PASSWORD_PATH}?token=${encodeURIComponent(token)}`;
}

export function isMissingRecoveryToken(token: string | null | undefined): boolean {
  return !token || !token.trim();
}
