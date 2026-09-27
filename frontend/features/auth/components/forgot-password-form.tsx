"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { requestPasswordRecovery } from "@/features/auth/api";
import { forgotPasswordEmailError } from "@/features/auth/password-recovery-presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { TextField } from "@/shared/ui/text-field";

export function ForgotPasswordForm() {
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState<string | undefined>();
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitAttempted(true);
    setError(null);
    const fieldError = forgotPasswordEmailError(email);
    setEmailError(fieldError);
    if (fieldError) {
      return;
    }

    setPending(true);
    try {
      const response = await requestPasswordRecovery({ email: email.trim() });
      setSuccessMessage(response.message);
    } catch (cause) {
      if (isApiError(cause)) {
        setError(messageForApiProblem(cause.problem));
      } else {
        setError("No se pudo enviar la solicitud.");
      }
    } finally {
      setPending(false);
    }
  }

  if (successMessage) {
    return (
      <div className="grid gap-4">
        <Alert tone="success" title="Revisa tu correo">
          {successMessage}
        </Alert>
        <p className="text-sm text-sf-muted">
          <Link href="/login" className="font-semibold text-sf-accent">
            Volver a iniciar sesión
          </Link>
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      {error ? (
        <Alert tone="error" title="No se pudo enviar">
          {error}
        </Alert>
      ) : null}
      <TextField
        id="forgot-password-email"
        label="Correo electrónico"
        type="email"
        name="email"
        autoComplete="email"
        required
        value={email}
        error={submitAttempted ? emailError : undefined}
        onChange={(event) => {
          setEmail(event.target.value);
          if (submitAttempted) {
            setEmailError(forgotPasswordEmailError(event.target.value));
          }
        }}
      />
      <Button type="submit" disabled={pending}>
        {pending ? "Enviando…" : "Enviar enlace"}
      </Button>
      <p className="text-sm text-sf-muted">
        <Link href="/login" className="font-semibold text-sf-accent">
          Volver a iniciar sesión
        </Link>
      </p>
    </form>
  );
}
