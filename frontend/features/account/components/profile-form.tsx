"use client";

import { useId, useState, type FormEvent } from "react";
import type {
  CustomerProfile,
  UpdateCustomerProfileRequest,
} from "@/features/account/api";
import { REGISTER_DOCUMENT_TYPES } from "@/features/auth/api";
import {
  REGISTER_DOCUMENT_NUMBER_MAX,
  digitsOnly,
  validateDocumentNumber,
  validateDocumentType,
  validateEmail,
  validateFirstName,
  validateLastName,
  validatePhone,
} from "@/features/auth/register-validation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { SelectField } from "@/shared/ui/select-field";
import { TextField } from "@/shared/ui/text-field";

type ProfileFormValues = {
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
};

/**
 * Mapea el error del servidor a feedback utilizable: duplicados de documento
 * y correo van al campo correspondiente; el resto se muestra como aviso
 * general (los 400 llegan con detalle específico del backend).
 */
export function profileServerFeedback(error: unknown): {
  fieldErrors: Partial<ProfileFormValues>;
  general: string | null;
} {
  if (!isApiError(error)) {
    return {
      fieldErrors: {},
      general: "No se pudo actualizar el perfil. Inténtalo de nuevo.",
    };
  }
  const message = messageForApiProblem(error.problem);
  if (error.problem.code === "DOCUMENT_ALREADY_EXISTS") {
    return { fieldErrors: { documentNumber: message }, general: null };
  }
  if (error.problem.code === "USER_ALREADY_EXISTS") {
    return { fieldErrors: { email: message }, general: null };
  }
  return { fieldErrors: {}, general: message };
}

export function ProfileForm({
  profile,
  pending,
  saved,
  error,
  onSubmit,
}: {
  profile: CustomerProfile;
  pending: boolean;
  saved: boolean;
  error: unknown;
  onSubmit: (body: UpdateCustomerProfileRequest) => void;
}) {
  const formId = useId();
  const [values, setValues] = useState<ProfileFormValues>(() =>
    profileToValues(profile),
  );
  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<keyof ProfileFormValues, string>>
  >({});
  const serverFeedback = profileServerFeedback(error);

  function update<K extends keyof ProfileFormValues>(
    key: K,
    value: ProfileFormValues[K],
  ) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending) {
      return;
    }
    const nextErrors = validate(values);
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }
    onSubmit({
      documentType: values.documentType,
      documentNumber: values.documentNumber.trim(),
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim(),
      phone: values.phone.trim(),
    });
  }

  const fieldError = (key: keyof ProfileFormValues): string | undefined =>
    fieldErrors[key] ?? serverFeedback.fieldErrors[key];

  return (
    <form className="grid gap-3" onSubmit={handleSubmit} noValidate>
      {saved && !pending ? (
        <Alert tone="success" title="Perfil actualizado">
          Tus datos se guardaron correctamente.
        </Alert>
      ) : null}
      {serverFeedback.general ? (
        <Alert tone="error" title="No se pudo actualizar el perfil">
          {serverFeedback.general}
        </Alert>
      ) : null}
      <div className="grid gap-3 sm:grid-cols-2">
        <SelectField
          id={`${formId}-document-type`}
          label="Tipo de documento"
          name="documentType"
          required
          value={values.documentType}
          error={fieldError("documentType")}
          onChange={(event) => update("documentType", event.target.value)}
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
          id={`${formId}-document-number`}
          label="Número de documento"
          inputMode="numeric"
          autoComplete="off"
          required
          maxLength={REGISTER_DOCUMENT_NUMBER_MAX}
          value={values.documentNumber}
          error={fieldError("documentNumber")}
          onChange={(event) =>
            update("documentNumber", digitsOnly(event.target.value))
          }
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <TextField
          id={`${formId}-first-name`}
          label="Nombre"
          autoComplete="given-name"
          required
          value={values.firstName}
          error={fieldError("firstName")}
          onChange={(event) => update("firstName", event.target.value)}
        />
        <TextField
          id={`${formId}-last-name`}
          label="Apellidos"
          autoComplete="family-name"
          required
          value={values.lastName}
          error={fieldError("lastName")}
          onChange={(event) => update("lastName", event.target.value)}
        />
      </div>
      <TextField
        id={`${formId}-email`}
        label="Correo electrónico"
        type="email"
        autoComplete="email"
        required
        value={values.email}
        error={fieldError("email")}
        onChange={(event) => update("email", event.target.value)}
      />
      <TextField
        id={`${formId}-phone`}
        label="Celular"
        type="tel"
        inputMode="numeric"
        autoComplete="tel"
        required
        value={values.phone}
        error={fieldError("phone")}
        onChange={(event) => update("phone", event.target.value)}
      />
      <div className="mt-1 flex flex-col gap-2 sm:flex-row sm:items-center">
        <Button type="submit" disabled={pending}>
          {pending ? "Guardando…" : "Guardar cambios"}
        </Button>
      </div>
    </form>
  );
}

function profileToValues(profile: CustomerProfile): ProfileFormValues {
  return {
    documentType: profile.documentType,
    documentNumber: profile.documentNumber,
    firstName: profile.firstName,
    lastName: profile.lastName,
    email: profile.email,
    phone: profile.phone,
  };
}

function validate(
  values: ProfileFormValues,
): Partial<Record<keyof ProfileFormValues, string>> {
  const entries: Array<[keyof ProfileFormValues, string | null]> = [
    ["documentType", validateDocumentType(values.documentType)],
    ["documentNumber", validateDocumentNumber(values.documentNumber)],
    ["firstName", validateFirstName(values.firstName)],
    ["lastName", validateLastName(values.lastName)],
    ["email", validateEmail(values.email)],
    ["phone", validatePhone(values.phone)],
  ];
  const errors: Partial<Record<keyof ProfileFormValues, string>> = {};
  for (const [key, message] of entries) {
    if (message) {
      errors[key] = message;
    }
  }
  return errors;
}
