"use client";

import { useId, useState, type FormEvent } from "react";
import type {
  AdminProductVariantFieldErrors,
  AdminProductVariantFormValues,
} from "@/features/admin/payloads";
import { Button } from "@/shared/ui/button";
import { TextField } from "@/shared/ui/text-field";

export function AdminProductVariantForm({
  mode,
  values,
  pending,
  error,
  onChange,
  onSubmit,
  onCancel,
  validate,
}: {
  mode: "create" | "edit";
  values: AdminProductVariantFormValues;
  pending: boolean;
  error: string | null;
  onChange: (values: AdminProductVariantFormValues) => void;
  onSubmit: () => void;
  onCancel: () => void;
  validate: (
    values: AdminProductVariantFormValues,
  ) => AdminProductVariantFieldErrors;
}) {
  const formId = useId();
  const [fieldErrors, setFieldErrors] = useState<AdminProductVariantFieldErrors>(
    {},
  );

  function update<K extends keyof AdminProductVariantFormValues>(
    key: K,
    value: AdminProductVariantFormValues[K],
  ) {
    onChange({ ...values, [key]: value });
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validate(values);
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }
    onSubmit();
  }

  return (
    <form className="grid gap-4" onSubmit={handleSubmit} noValidate>
      <TextField
        id={`${formId}-name`}
        label="Nombre"
        value={values.name}
        error={fieldErrors.name}
        autoComplete="off"
        onChange={(event) => update("name", event.target.value)}
      />
      <div className="grid gap-1">
        <label
          htmlFor={`${formId}-description`}
          className="text-sm font-semibold"
        >
          Descripción (opcional)
        </label>
        <textarea
          id={`${formId}-description`}
          value={values.description}
          rows={3}
          className="rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-base text-sf-ink"
          onChange={(event) => update("description", event.target.value)}
        />
      </div>
      {error ? <p className="text-sm text-sf-error">{error}</p> : null}
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="submit" className="flex-1" disabled={pending}>
          {pending
            ? "Guardando…"
            : mode === "create"
              ? "Crear variante"
              : "Guardar cambios"}
        </Button>
        <Button
          type="button"
          variant="secondary"
          className="flex-1"
          disabled={pending}
          onClick={onCancel}
        >
          Cancelar
        </Button>
      </div>
    </form>
  );
}
