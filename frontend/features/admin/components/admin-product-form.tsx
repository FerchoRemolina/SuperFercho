"use client";

import { useId, useState, type FormEvent } from "react";
import type { Category } from "@/features/catalog/api";
import {
  type AdminProductFieldErrors,
  type AdminProductFormValues,
} from "@/features/admin/payloads";
import { productStatusLabel } from "@/features/admin/presentation";
import { Button } from "@/shared/ui/button";
import { SelectField } from "@/shared/ui/select-field";
import { TextField } from "@/shared/ui/text-field";

export function AdminProductForm({
  mode,
  values,
  categories,
  pending,
  error,
  onChange,
  onSubmit,
  onCancel,
  validate,
}: {
  mode: "create" | "edit";
  values: AdminProductFormValues;
  categories: Category[];
  pending: boolean;
  error: string | null;
  onChange: (values: AdminProductFormValues) => void;
  onSubmit: () => void;
  onCancel: () => void;
  validate: (values: AdminProductFormValues) => AdminProductFieldErrors;
}) {
  const formId = useId();
  const [fieldErrors, setFieldErrors] = useState<AdminProductFieldErrors>({});

  function update<K extends keyof AdminProductFormValues>(
    key: K,
    value: AdminProductFormValues[K],
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
      <SelectField
        id={`${formId}-category`}
        label="Categoría"
        value={values.categoryId}
        error={fieldErrors.categoryId}
        onChange={(event) => update("categoryId", event.target.value)}
      >
        <option value="">Selecciona una categoría</option>
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.status === "INACTIVE"
              ? `${category.name} (${productStatusLabel("INACTIVE")})`
              : category.name}
          </option>
        ))}
      </SelectField>
      <TextField
        id={`${formId}-brand`}
        label="Marca (opcional)"
        value={values.brand}
        autoComplete="off"
        onChange={(event) => update("brand", event.target.value)}
      />
      <TextField
        id={`${formId}-barcode`}
        label="Código de barras (opcional)"
        value={values.barcode}
        autoComplete="off"
        onChange={(event) => update("barcode", event.target.value)}
      />
      <div className="grid gap-1">
        <label htmlFor={`${formId}-description`} className="text-sm font-semibold">
          Descripción (opcional)
        </label>
        <textarea
          id={`${formId}-description`}
          value={values.description}
          rows={4}
          className="rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-base text-sf-ink"
          onChange={(event) => update("description", event.target.value)}
        />
      </div>
      <TextField
        id={`${formId}-image`}
        label="URL de imagen (opcional)"
        value={values.imageUrl}
        autoComplete="off"
        onChange={(event) => update("imageUrl", event.target.value)}
      />
      {mode === "create" ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            id={`${formId}-price`}
            label="Precio (COP)"
            inputMode="decimal"
            value={values.price}
            error={fieldErrors.price}
            onChange={(event) => update("price", event.target.value)}
          />
          <TextField
            id={`${formId}-stock`}
            label="Stock inicial"
            inputMode="numeric"
            value={values.stock}
            error={fieldErrors.stock}
            onChange={(event) => update("stock", event.target.value)}
          />
        </div>
      ) : null}
      {error ? <p className="text-sm text-sf-error">{error}</p> : null}
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button type="submit" className="flex-1" disabled={pending}>
          {pending
            ? "Guardando…"
            : mode === "create"
              ? "Crear producto"
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
