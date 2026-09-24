"use client";

import { useId, useState, type FormEvent } from "react";
import type {
  AddAddressRequest,
  Address,
  UpdateAddressRequest,
} from "@/features/account/api";
import { Button } from "@/shared/ui/button";
import { TextField } from "@/shared/ui/text-field";

type AddressFormValues = {
  label: string;
  recipientName: string;
  addressLine: string;
  additionalInfo: string;
  city: string;
  department: string;
  phone: string;
  isDefault: boolean;
};

export function AddressForm({
  address,
  pending,
  error,
  onSubmitCreate,
  onSubmitUpdate,
  onCancel,
}: {
  address?: Address;
  pending: boolean;
  error: string | null;
  onSubmitCreate: (body: AddAddressRequest) => void;
  onSubmitUpdate: (addressId: string, body: UpdateAddressRequest) => void;
  onCancel: () => void;
}) {
  const formId = useId();
  const editing = address !== undefined;
  const [values, setValues] = useState<AddressFormValues>(() =>
    addressToValues(address),
  );
  const [fieldErrors, setFieldErrors] = useState<Partial<AddressFormValues>>(
    {},
  );

  function update<K extends keyof AddressFormValues>(
    key: K,
    value: AddressFormValues[K],
  ) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validate(values);
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    const additionalInfo = values.additionalInfo.trim();
    const shared = {
      label: values.label.trim(),
      recipientName: values.recipientName.trim(),
      addressLine: values.addressLine.trim(),
      additionalInfo: additionalInfo.length > 0 ? additionalInfo : null,
      city: values.city.trim(),
      department: values.department.trim(),
      phone: values.phone.trim(),
    };

    if (editing) {
      onSubmitUpdate(address.id, shared);
      return;
    }

    onSubmitCreate({ ...shared, isDefault: values.isDefault });
  }

  return (
    <form className="grid gap-3" onSubmit={handleSubmit} noValidate>
      <TextField
        id={`${formId}-label`}
        label="Nombre de la dirección"
        value={values.label}
        error={fieldErrors.label}
        autoComplete="off"
        onChange={(event) => update("label", event.target.value)}
      />
      <TextField
        id={`${formId}-recipient`}
        label="Destinatario"
        value={values.recipientName}
        error={fieldErrors.recipientName}
        autoComplete="name"
        onChange={(event) => update("recipientName", event.target.value)}
      />
      <TextField
        id={`${formId}-line`}
        label="Dirección"
        value={values.addressLine}
        error={fieldErrors.addressLine}
        autoComplete="street-address"
        onChange={(event) => update("addressLine", event.target.value)}
      />
      <TextField
        id={`${formId}-extra`}
        label="Información adicional (opcional)"
        value={values.additionalInfo}
        autoComplete="off"
        onChange={(event) => update("additionalInfo", event.target.value)}
      />
      <div className="grid gap-3 sm:grid-cols-2">
        <TextField
          id={`${formId}-city`}
          label="Ciudad"
          value={values.city}
          error={fieldErrors.city}
          autoComplete="address-level2"
          onChange={(event) => update("city", event.target.value)}
        />
        <TextField
          id={`${formId}-department`}
          label="Departamento"
          value={values.department}
          error={fieldErrors.department}
          autoComplete="address-level1"
          onChange={(event) => update("department", event.target.value)}
        />
      </div>
      <TextField
        id={`${formId}-phone`}
        label="Teléfono"
        value={values.phone}
        error={fieldErrors.phone}
        type="tel"
        autoComplete="tel"
        onChange={(event) => update("phone", event.target.value)}
      />
      {editing ? null : (
        <label className="flex min-h-11 items-center gap-2 text-sm font-semibold text-sf-ink">
          <input
            type="checkbox"
            className="size-4 accent-sf-primary"
            checked={values.isDefault}
            onChange={(event) => update("isDefault", event.target.checked)}
          />
          Usar como dirección predeterminada
        </label>
      )}
      {error ? <p className="text-sm text-sf-error">{error}</p> : null}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <Button type="submit" disabled={pending}>
          {pending
            ? "Guardando…"
            : editing
              ? "Guardar cambios"
              : "Guardar dirección"}
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={pending}
          onClick={onCancel}
        >
          Cancelar
        </Button>
      </div>
    </form>
  );
}

function addressToValues(address: Address | undefined): AddressFormValues {
  return {
    label: address?.label ?? "",
    recipientName: address?.recipientName ?? "",
    addressLine: address?.addressLine ?? "",
    additionalInfo: address?.additionalInfo ?? "",
    city: address?.city ?? "",
    department: address?.department ?? "",
    phone: address?.phone ?? "",
    isDefault: address?.isDefault ?? false,
  };
}

function validate(
  values: AddressFormValues,
): Partial<AddressFormValues> {
  const errors: Partial<AddressFormValues> = {};
  if (values.label.trim().length === 0) {
    errors.label = "Escribe un nombre para identificar la dirección.";
  }
  if (values.recipientName.trim().length === 0) {
    errors.recipientName = "Escribe el nombre del destinatario.";
  }
  if (values.addressLine.trim().length === 0) {
    errors.addressLine = "Escribe la dirección.";
  }
  if (values.city.trim().length === 0) {
    errors.city = "Escribe la ciudad.";
  }
  if (values.department.trim().length === 0) {
    errors.department = "Escribe el departamento.";
  }
  if (values.phone.trim().length === 0) {
    errors.phone = "Escribe el teléfono.";
  }
  return errors;
}
