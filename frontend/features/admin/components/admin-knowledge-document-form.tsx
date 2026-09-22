"use client";

import { useId, useState, type FormEvent } from "react";
import type {
  AdminKnowledgeDocumentFieldErrors,
  AdminKnowledgeDocumentFormValues,
} from "@/features/admin/payloads";
import { Button } from "@/shared/ui/button";
import { TextField } from "@/shared/ui/text-field";
import { cx } from "@/shared/utils/cx";

export function AdminKnowledgeDocumentForm({
  values,
  pending,
  onChange,
  onSubmit,
  onCancel,
  validate,
}: {
  values: AdminKnowledgeDocumentFormValues;
  pending: boolean;
  onChange: (values: AdminKnowledgeDocumentFormValues) => void;
  onSubmit: () => void;
  onCancel: () => void;
  validate: (
    values: AdminKnowledgeDocumentFormValues,
  ) => AdminKnowledgeDocumentFieldErrors;
}) {
  const formId = useId();
  const [fieldErrors, setFieldErrors] = useState<AdminKnowledgeDocumentFieldErrors>(
    {},
  );

  function update<K extends keyof AdminKnowledgeDocumentFormValues>(
    key: K,
    value: AdminKnowledgeDocumentFormValues[K],
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
        id={`${formId}-title`}
        label="Título"
        value={values.title}
        error={fieldErrors.title}
        autoComplete="off"
        disabled={pending}
        onChange={(event) => update("title", event.target.value)}
      />
      <TextField
        id={`${formId}-source`}
        label="Fuente"
        value={values.source}
        error={fieldErrors.source}
        autoComplete="off"
        disabled={pending}
        onChange={(event) => update("source", event.target.value)}
      />
      <div className="grid gap-1">
        <label htmlFor={`${formId}-content`} className="text-sm font-semibold">
          Contenido
        </label>
        <textarea
          id={`${formId}-content`}
          value={values.content}
          rows={10}
          disabled={pending}
          aria-invalid={fieldErrors.content ? true : undefined}
          aria-describedby={
            fieldErrors.content ? `${formId}-content-error` : undefined
          }
          className={cx(
            "rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-base text-sf-ink disabled:opacity-60",
            fieldErrors.content && "border-sf-error",
          )}
          onChange={(event) => update("content", event.target.value)}
        />
        {fieldErrors.content ? (
          <p id={`${formId}-content-error`} className="text-sm text-sf-error">
            {fieldErrors.content}
          </p>
        ) : null}
      </div>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="submit" className="flex-1" disabled={pending}>
          {pending ? "Creando…" : "Crear documento"}
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
