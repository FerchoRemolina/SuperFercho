"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { safeNextPath } from "@/shared/auth/safe-next-path";
import { useSession } from "@/shared/session/session-provider";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { PasswordField } from "@/shared/ui/password-field";
import { SelectField } from "@/shared/ui/select-field";
import { TextField } from "@/shared/ui/text-field";
import {
  homePathForRole,
  REGISTER_DOCUMENT_TYPES,
  registerCustomer,
} from "@/features/auth/api";
import { PasswordRequirementsList } from "@/features/auth/components/password-requirements-list";
import { shouldShowPasswordRequirements } from "@/features/auth/password-recovery-presentation";
import {
  digitsOnly,
  isPasswordValid,
  normalizeRegisterPersonName,
  REGISTER_DOCUMENT_NUMBER_MAX,
  REGISTER_NAME_MAX,
  REGISTER_PASSWORD_MAX,
  REGISTER_PHONE_LENGTH,
  validateRegisterForm,
  type RegisterFieldErrors,
  type RegisterFormValues,
} from "@/features/auth/register-validation";

type TouchedFields = Partial<Record<keyof RegisterFormValues, boolean>>;

function fieldError(
  errors: RegisterFieldErrors,
  field: keyof RegisterFormValues,
  touched: TouchedFields,
  submitAttempted: boolean,
): string | undefined {
  if (!(submitAttempted || touched[field])) {
    return undefined;
  }
  return errors[field];
}

export function RegisterForm() {
  const { session } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = safeNextPath(searchParams.get("next"));
  const [documentType, setDocumentType] = useState("");
  const [documentNumber, setDocumentNumber] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [passwordHintsVisible, setPasswordHintsVisible] = useState(false);
  const [touched, setTouched] = useState<TouchedFields>({});
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const values: RegisterFormValues = useMemo(
    () => ({
      documentType,
      documentNumber,
      firstName,
      lastName,
      email,
      phone,
      password,
    }),
    [documentNumber, documentType, email, firstName, lastName, password, phone],
  );

  const errors = useMemo(() => validateRegisterForm(values), [values]);

  const showRequirements = shouldShowPasswordRequirements({
    interacted: passwordHintsVisible,
    submitAttempted,
    passwordLength: password.length,
  });

  useEffect(() => {
    if (session) {
      router.replace(nextPath ?? homePathForRole(session.role));
    }
  }, [nextPath, router, session]);

  function markTouched(field: keyof RegisterFormValues) {
    setTouched((current) => ({ ...current, [field]: true }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitAttempted(true);
    setError(null);

    const nextErrors = validateRegisterForm(values);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setPending(true);
    try {
      await registerCustomer({
        documentType,
        documentNumber,
        firstName: normalizeRegisterPersonName(firstName),
        lastName: normalizeRegisterPersonName(lastName),
        email: email.trim().toLowerCase(),
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
        error={fieldError(errors, "documentType", touched, submitAttempted)}
        onBlur={() => markTouched("documentType")}
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
        inputMode="numeric"
        autoComplete="off"
        required
        maxLength={REGISTER_DOCUMENT_NUMBER_MAX}
        value={documentNumber}
        error={fieldError(errors, "documentNumber", touched, submitAttempted)}
        onBlur={() => markTouched("documentNumber")}
        onChange={(event) =>
          setDocumentNumber(
            digitsOnly(event.target.value).slice(0, REGISTER_DOCUMENT_NUMBER_MAX),
          )
        }
      />
      <TextField
        id="register-first-name"
        label="Nombre"
        name="firstName"
        autoComplete="given-name"
        required
        maxLength={REGISTER_NAME_MAX}
        value={firstName}
        error={fieldError(errors, "firstName", touched, submitAttempted)}
        onBlur={() => markTouched("firstName")}
        onChange={(event) =>
          setFirstName(event.target.value.slice(0, REGISTER_NAME_MAX))
        }
      />
      <TextField
        id="register-last-name"
        label="Apellido"
        name="lastName"
        autoComplete="family-name"
        required
        maxLength={REGISTER_NAME_MAX}
        value={lastName}
        error={fieldError(errors, "lastName", touched, submitAttempted)}
        onBlur={() => markTouched("lastName")}
        onChange={(event) =>
          setLastName(event.target.value.slice(0, REGISTER_NAME_MAX))
        }
      />
      <TextField
        id="register-email"
        label="Correo"
        type="email"
        name="email"
        autoComplete="email"
        required
        value={email}
        error={fieldError(errors, "email", touched, submitAttempted)}
        onBlur={() => markTouched("email")}
        onChange={(event) => setEmail(event.target.value)}
      />
      <TextField
        id="register-phone"
        label="Celular"
        type="tel"
        name="phone"
        inputMode="numeric"
        autoComplete="tel"
        required
        maxLength={REGISTER_PHONE_LENGTH}
        value={phone}
        error={fieldError(errors, "phone", touched, submitAttempted)}
        onBlur={() => markTouched("phone")}
        onChange={(event) =>
          setPhone(digitsOnly(event.target.value).slice(0, REGISTER_PHONE_LENGTH))
        }
      />
      <PasswordField
        id="register-password"
        label="Contraseña"
        name="password"
        autoComplete="new-password"
        required
        maxLength={REGISTER_PASSWORD_MAX}
        value={password}
        error={fieldError(errors, "password", touched, submitAttempted)}
        valid={
          (submitAttempted || touched.password) && isPasswordValid(password)
        }
        onFocus={() => setPasswordHintsVisible(true)}
        onBlur={() => markTouched("password")}
        onChange={(event) => {
          setPasswordHintsVisible(true);
          setPassword(event.target.value.slice(0, REGISTER_PASSWORD_MAX));
        }}
        description={
          showRequirements ? (
            <PasswordRequirementsList password={password} />
          ) : undefined
        }
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
