"use client";

import { useId, useState, type FormEvent } from "react";
import type {
  AdminKnowledgeDocumentContentFieldErrors,
  AdminKnowledgeDocumentContentFormValues,
} from "@/features/admin/payloads";
import { knowledgeDocumentReplaceContentWarning } from "@/features/admin/presentation";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

export function AdminKnowledgeDocumentContentForm({
  values,
  pending,
  error,
  onChange,
  onSubmit,
  validate,
}: {
  values: AdminKnowledgeDocumentContentFormValues;
  pending: boolean;
  error: string | null;
  onChange: (values: AdminKnowledgeDocumentContentFormValues) => void;
  onSubmit: () => void;
  validate: (
    values: AdminKnowledgeDocumentContentFormValues,
  ) => AdminKnowledgeDocumentContentFieldErrors;
}) {
  const formId = useId();
  const [fieldErrors, setFieldErrors] =
    useState<AdminKnowledgeDocumentContentFieldErrors>({});
  const [confirming, setConfirming] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validate(values);
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setConfirming(false);
      return;
    }
    if (!confirming) {
      setConfirming(true);
      return;
    }
    onSubmit();
  }

  return (
    <form className="grid gap-4" onSubmit={handleSubmit} noValidate>
      <Alert tone="warning" title="Reemplazo de contenido">
        {knowledgeDocumentReplaceContentWarning()}
      </Alert>
      <div className="grid gap-1">
        <label htmlFor={`${formId}-content`} className="text-sm font-semibold">
          Contenido
        </label>
        <textarea
          id={`${formId}-content`}
          value={values.content}
          rows={12}
          disabled={pending}
          aria-invalid={fieldErrors.content ? true : undefined}
          aria-describedby={
            fieldErrors.content ? `${formId}-content-error` : undefined
          }
          className={cx(
            "rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-base text-sf-ink disabled:opacity-60",
            fieldErrors.content && "border-sf-error",
          )}
          onChange={(event) => {
            setConfirming(false);
            onChange({ content: event.target.value });
          }}
        />
        {fieldErrors.content ? (
          <p id={`${formId}-content-error`} className="text-sm text-sf-error">
            {fieldErrors.content}
          </p>
        ) : null}
      </div>
      {error ? (
        <Alert tone="error" title="No se pudo reemplazar el contenido">
          {error}
        </Alert>
      ) : null}
      {confirming ? (
        <div className="grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-3">
          <p className="text-sm font-semibold text-sf-ink">
            ¿Reemplazar el contenido?
          </p>
          <p className="text-sm text-sf-muted">
            El documento pasará a Recibido y deberás procesarlo de nuevo.
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button type="submit" disabled={pending} className="flex-1">
              {pending ? "Guardando…" : "Sí, reemplazar"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              disabled={pending}
              onClick={() => setConfirming(false)}
            >
              Cancelar
            </Button>
          </div>
        </div>
      ) : (
        <Button type="submit" disabled={pending}>
          Reemplazar contenido
        </Button>
      )}
    </form>
  );
}
