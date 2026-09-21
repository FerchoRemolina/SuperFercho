"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { safeNextPath } from "@/shared/auth/safe-next-path";
import { useSession } from "@/shared/session/session-provider";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { SelectField } from "@/shared/ui/select-field";
import { TextField } from "@/shared/ui/text-field";
import {
  homePathForRole,
  REGISTER_DOCUMENT_TYPES,
  registerCustomer,
} from "@/features/auth/api";

export function RegisterForm() {
  const { session } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = safeNextPath(searchParams.get("next"));
  const [documentType, setDocumentType] = useState("");
  const [documentNumber, setDocumentNumber] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (session) {
      router.replace(nextPath ?? homePathForRole(session.role));
    }
  }, [nextPath, router, session]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setPending(true);
    try {
      await registerCustomer({
        documentType,
        documentNumber,
        fullName,
        email,
        phone,
        password,
      });
      router.replace(
        nextPath
          ? `/login?registered=1&next=${encodeURIComponent(nextPath)}`
          : "/login?registered=1",
      );
    } catch (cause) {
      if (isApiError(cause)) {
        setError(messageForApiProblem(cause.problem));
      } else {
        setError("No se pudo crear la cuenta.");
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      {error ? (
        <Alert tone="error" title="No se pudo registrar">
          {error}
        </Alert>
      ) : null}
      <SelectField
        id="register-document-type"
        label="Tipo de documento"
        name="documentType"
        required
        value={documentType}
        onChange={(event) => setDocumentType(event.target.value)}
      >
        <option value="" disabled>
          Selecciona un tipo
        </option>
        {REGISTER_DOCUMENT_TYPES.map((type) => (
          <option key={type.value} value={type.value}>
            {type.label}
          </option>
        ))}
      </SelectField>
      <TextField
        id="register-document-number"
        label="Número de documento"
        name="documentNumber"
        autoComplete="off"
        required
        value={documentNumber}
        onChange={(event) => setDocumentNumber(event.target.value)}
      />
      <TextField
        id="register-full-name"
        label="Nombre completo"
        name="fullName"
        autoComplete="name"
        required
        value={fullName}
        onChange={(event) => setFullName(event.target.value)}
      />
      <TextField
        id="register-email"
        label="Correo"
        type="email"
        name="email"
        autoComplete="email"
        required
        value={email}
        onChange={(event) => setEmail(event.target.value)}
      />
      <TextField
        id="register-phone"
        label="Teléfono"
        type="tel"
        name="phone"
        autoComplete="tel"
        required
        value={phone}
        onChange={(event) => setPhone(event.target.value)}
      />
      <TextField
        id="register-password"
        label="Contraseña"
        type="password"
        name="password"
        autoComplete="new-password"
        required
        value={password}
        onChange={(event) => setPassword(event.target.value)}
      />
      <Button type="submit" disabled={pending}>
        {pending ? "Creando cuenta…" : "Crear cuenta"}
      </Button>
      <p className="text-sm text-sf-muted">
        ¿Ya tienes cuenta?{" "}
        <Link
          href={nextPath ? `/login?next=${encodeURIComponent(nextPath)}` : "/login"}
          className="font-semibold text-sf-accent"
        >
          Iniciar sesión
        </Link>
      </p>
    </form>
  );
}
