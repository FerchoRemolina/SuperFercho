"use client";

import { useId, useState, type FormEvent } from "react";
import { PRESENTATION_UNITS } from "@/features/admin/api";
import type { Category } from "@/features/catalog/api";
import {
  useAdminProductTypesQuery,
  useAdminProductVariantsQuery,
} from "@/features/admin/hooks";
import {
  type AdminProductFieldErrors,
  type AdminProductFormValues,
  PRODUCT_BRAND_MAX_LENGTH,
  PRODUCT_DESCRIPTION_MAX_LENGTH,
  PRODUCT_NAME_MAX_LENGTH,
  withAdminProductCategoryId,
  withAdminProductTypeId,
} from "@/features/admin/payloads";
import {
  productStatusLabel,
  productTypeStatusLabel,
} from "@/features/admin/presentation";
import { Button } from "@/shared/ui/button";
import { SelectField } from "@/shared/ui/select-field";
import { TextField } from "@/shared/ui/text-field";
import { cx } from "@/shared/utils/cx";

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
  const typesQuery = useAdminProductTypesQuery(values.categoryId);
  const variantsQuery = useAdminProductVariantsQuery(values.productTypeId);

  const hasCategory = values.categoryId.trim().length > 0;
  const hasType = values.productTypeId.trim().length > 0;
  const types = typesQuery.data ?? [];
  const variants = variantsQuery.data ?? [];
  const typesReady = hasCategory && typesQuery.isSuccess;
  const variantsReady = hasType && variantsQuery.isSuccess;

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
      <SelectField
        id={`${formId}-category`}
        label="Categoría"
        value={values.categoryId}
        error={fieldErrors.categoryId}
        onChange={(event) =>
          onChange(withAdminProductCategoryId(values, event.target.value))
        }
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

      <SelectField
        id={`${formId}-product-type`}
        label="Tipo de producto"
        value={values.productTypeId}
        error={fieldErrors.productTypeId}
        disabled={!hasCategory || typesQuery.isPending}
        onChange={(event) =>
          onChange(withAdminProductTypeId(values, event.target.value))
        }
      >
        <option value="">
          {!hasCategory
            ? "Selecciona primero una categoría"
            : typesQuery.isPending
              ? "Cargando tipos…"
              : "Selecciona un tipo"}
        </option>
        {types.map((productType) => (
          <option key={productType.id} value={productType.id}>
            {productType.status === "INACTIVE"
              ? `${productType.name} — ${productTypeStatusLabel("INACTIVE")}`
              : productType.name}
          </option>
        ))}
      </SelectField>
      {typesReady && types.length === 0 ? (
        <p className="text-sm text-sf-muted">
          Esta categoría no tiene tipos de producto. Crea un tipo en el detalle
          de la categoría antes de continuar.
        </p>
      ) : null}
      {hasCategory && typesQuery.isError ? (
        <p className="text-sm text-sf-error">
          No se pudieron cargar los tipos de producto.
        </p>
      ) : null}

      <SelectField
        id={`${formId}-product-variant`}
        label="Variante (opcional)"
        value={values.productVariantId}
        error={fieldErrors.productVariantId}
        disabled={!hasType || variantsQuery.isPending}
        onChange={(event) => update("productVariantId", event.target.value)}
      >
        <option value="">
          {!hasType
            ? "Selecciona primero un tipo"
            : variantsQuery.isPending
              ? "Cargando variantes…"
              : "Sin variante"}
        </option>
        {variants.map((productVariant) => (
          <option key={productVariant.id} value={productVariant.id}>
            {productVariant.status === "INACTIVE"
              ? `${productVariant.name} — Inactiva`
              : productVariant.name}
          </option>
        ))}
      </SelectField>
      {variantsReady && variants.length === 0 ? (
        <p className="text-sm text-sf-muted">
          Este tipo no tiene variantes. Puedes guardar el producto sin variante.
        </p>
      ) : null}
      {hasType && variantsQuery.isError ? (
        <p className="text-sm text-sf-error">
          No se pudieron cargar las variantes.
        </p>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          id={`${formId}-presentation-quantity`}
          label="Cantidad de presentación"
          inputMode="decimal"
          value={values.presentationQuantity}
          error={fieldErrors.presentationQuantity}
          onChange={(event) =>
            update("presentationQuantity", event.target.value)
          }
        />
        <SelectField
          id={`${formId}-presentation-unit`}
          label="Unidad"
          value={values.presentationUnit}
          error={fieldErrors.presentationUnit}
          onChange={(event) =>
            update(
              "presentationUnit",
              event.target.value as AdminProductFormValues["presentationUnit"],
            )
          }
        >
          <option value="">Selecciona una unidad</option>
          {PRESENTATION_UNITS.map((unit) => (
            <option key={unit} value={unit}>
              {unit}
            </option>
          ))}
        </SelectField>
      </div>

      <TextField
        id={`${formId}-name`}
        label="Nombre"
        value={values.name}
        error={fieldErrors.name}
        autoComplete="off"
        maxLength={PRODUCT_NAME_MAX_LENGTH}
        onChange={(event) => update("name", event.target.value)}
      />
      <TextField
        id={`${formId}-brand`}
        label="Marca (opcional)"
        value={values.brand}
        error={fieldErrors.brand}
        autoComplete="off"
        maxLength={PRODUCT_BRAND_MAX_LENGTH}
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
          maxLength={PRODUCT_DESCRIPTION_MAX_LENGTH}
          aria-invalid={fieldErrors.description ? true : undefined}
          aria-describedby={
            fieldErrors.description ? `${formId}-description-error` : undefined
          }
          className={cx(
            "rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-base text-sf-ink",
            fieldErrors.description && "border-sf-error",
          )}
          onChange={(event) => update("description", event.target.value)}
        />
        {fieldErrors.description ? (
          <p id={`${formId}-description-error`} className="text-sm text-sf-error">
            {fieldErrors.description}
          </p>
        ) : null}
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
