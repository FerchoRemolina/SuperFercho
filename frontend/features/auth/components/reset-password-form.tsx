"use client";

import { useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { resetPassword } from "@/features/auth/api";
import { PasswordRequirementsList } from "@/features/auth/components/password-requirements-list";
import {
  isMissingRecoveryToken,
  resetConfirmPasswordError,
  shouldShowPasswordRequirements,
} from "@/features/auth/password-recovery-presentation";
import {
  isPasswordValid,
  validatePassword,
} from "@/features/auth/register-validation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { PasswordField } from "@/shared/ui/password-field";

export function ResetPasswordForm({ token }: { token: string }) {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordHintsVisible, setPasswordHintsVisible] = useState(false);
  const [touched, setTouched] = useState({
    password: false,
    confirmPassword: false,
  });
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const passwordError = useMemo(() => {
    if (!(submitAttempted || touched.password)) {
      return undefined;
    }
    return validatePassword(password) ?? undefined;
  }, [password, submitAttempted, touched.password]);

  const confirmError = useMemo(() => {
    if (!(submitAttempted || touched.confirmPassword)) {
      return undefined;
    }
    return resetConfirmPasswordError(password, confirmPassword);
  }, [confirmPassword, password, submitAttempted, touched.confirmPassword]);

  const showRequirements = shouldShowPasswordRequirements({
    interacted: passwordHintsVisible,
    submitAttempted,
    passwordLength: password.length,
  });

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitAttempted(true);
    setError(null);

    const nextPasswordError = validatePassword(password);
    const nextConfirmError = resetConfirmPasswordError(password, confirmPassword);

    if (nextPasswordError || nextConfirmError) {
      return;
    }

    setPending(true);
    try {
      await resetPassword({
        token,
        newPassword: password,
        confirmPassword,
      });
      setDone(true);
      window.setTimeout(() => {
        router.replace("/login");
      }, 1800);
    } catch (cause) {
      if (isApiError(cause)) {
        setError(messageForApiProblem(cause.problem));
      } else {
        setError("No se pudo restablecer la contraseña.");
      }
    } finally {
      setPending(false);
    }
  }

  if (isMissingRecoveryToken(token)) {
    return (
      <div className="grid gap-4">
        <Alert tone="error" title="Enlace no válido">
          Este enlace de recuperación no es válido. Solicita uno nuevo.
        </Alert>
        <p className="text-sm text-sf-muted">
          <Link href="/forgot-password" className="font-semibold text-sf-accent">
            Recuperar contraseña
          </Link>
        </p>
      </div>
    );
  }

  if (done) {
    return (
      <Alert tone="success" title="Contraseña actualizada">
        Ya puedes iniciar sesión con tu nueva contraseña.
      </Alert>
    );
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      {error ? (
        <Alert tone="error" title="No se pudo restablecer">
          {error}
        </Alert>
      ) : null}
      <PasswordField
        id="reset-password"
        label="Nueva contraseña"
        name="newPassword"
        autoComplete="new-password"
        required
        maxLength={12}
        value={password}
        error={passwordError}
        valid={
          (submitAttempted || touched.password) && isPasswordValid(password)
        }
        onFocus={() => setPasswordHintsVisible(true)}
        onBlur={() => setTouched((current) => ({ ...current, password: true }))}
        onChange={(event) => {
          setPasswordHintsVisible(true);
          setPassword(event.target.value.slice(0, 12));
        }}
        description={
          showRequirements ? (
            <PasswordRequirementsList password={password} />
          ) : undefined
        }
      />
      <PasswordField
        id="reset-password-confirm"
        label="Confirmar nueva contraseña"
        name="confirmPassword"
        autoComplete="new-password"
        required
        maxLength={12}
        value={confirmPassword}
        error={confirmError}
        valid={
          (submitAttempted || touched.confirmPassword) &&
          Boolean(confirmPassword) &&
          confirmPassword === password &&
          isPasswordValid(password)
        }
        onBlur={() =>
          setTouched((current) => ({ ...current, confirmPassword: true }))
        }
        onChange={(event) =>
          setConfirmPassword(event.target.value.slice(0, 12))
        }
      />
      <Button type="submit" disabled={pending}>
        {pending ? "Guardando…" : "Guardar contraseña"}
      </Button>
      <p className="text-sm text-sf-muted">
        <Link href="/login" className="font-semibold text-sf-accent">
          Volver a iniciar sesión
        </Link>
      </p>
    </form>
  );
}
