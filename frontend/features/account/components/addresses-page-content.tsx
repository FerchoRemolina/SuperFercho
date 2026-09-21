"use client";

import { useState } from "react";
import type { Address } from "@/features/account/api";
import { AddressForm } from "@/features/account/components/address-form";
import {
  useAddAddressMutation,
  useAddressesQuery,
  useDeactivateAddressMutation,
  useSetDefaultAddressMutation,
  useUpdateAddressMutation,
} from "@/features/account/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Badge } from "@/shared/ui/badge";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

type Editor =
  | { mode: "create" }
  | { mode: "edit"; address: Address }
  | null;

export function AddressesPageContent() {
  const addressesQuery = useAddressesQuery();
  const addMutation = useAddAddressMutation();
  const updateMutation = useUpdateAddressMutation();
  const deactivateMutation = useDeactivateAddressMutation();
  const defaultMutation = useSetDefaultAddressMutation();
  const [editor, setEditor] = useState<Editor>(null);
  const [pendingDeactivateId, setPendingDeactivateId] = useState<string | null>(
    null,
  );

  if (addressesQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Header />
        <div className="mt-8 grid gap-4">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      </Container>
    );
  }

  if (addressesQuery.isError) {
    const message = isApiError(addressesQuery.error)
      ? messageForApiProblem(addressesQuery.error.problem)
      : "No se pudieron cargar las direcciones.";
    return (
      <Container as="main" className="py-10 md:py-16">
        <Header />
        <div className="mt-8 grid gap-4">
          <Alert tone="error" title="No se pudieron cargar las direcciones">
            {message}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => addressesQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      </Container>
    );
  }

  const addresses = addressesQuery.data ?? [];
  const mutationError =
    mutationMessage(addMutation.error) ??
    mutationMessage(updateMutation.error) ??
    mutationMessage(deactivateMutation.error) ??
    mutationMessage(defaultMutation.error);
  const saving =
    addMutation.isPending ||
    updateMutation.isPending ||
    deactivateMutation.isPending ||
    defaultMutation.isPending;

  return (
    <Container as="main" className="py-10 md:py-16">
      <Header />
      {mutationError && editor === null ? (
        <div className="mt-6">
          <Alert tone="error" title="No se pudo actualizar">
            {mutationError}
          </Alert>
        </div>
      ) : null}

      {editor ? (
        <Card className="mt-8">
          <h2 className="text-xl font-semibold text-sf-ink">
            {editor.mode === "create" ? "Nueva dirección" : "Editar dirección"}
          </h2>
          <div className="mt-4">
            <AddressForm
              address={editor.mode === "edit" ? editor.address : undefined}
              pending={saving}
              error={mutationError}
              onCancel={() => setEditor(null)}
              onSubmitCreate={(body) => {
                addMutation.mutate(body, {
                  onSuccess: () => setEditor(null),
                });
              }}
              onSubmitUpdate={(addressId, body) => {
                updateMutation.mutate(
                  { addressId, body },
                  { onSuccess: () => setEditor(null) },
                );
              }}
            />
          </div>
        </Card>
      ) : (
        <div className="mt-6">
          <Button type="button" onClick={() => setEditor({ mode: "create" })}>
            Agregar dirección
          </Button>
        </div>
      )}

      {addresses.length === 0 && editor === null ? (
        <div className="mt-8">
          <EmptyState
            title="No tienes direcciones"
            description="Agrega una dirección para usarla al realizar un pedido."
            action={
              <button
                type="button"
                className={buttonClassName("primary")}
                onClick={() => setEditor({ mode: "create" })}
              >
                Agregar dirección
              </button>
            }
          />
        </div>
      ) : (
        <ul className="mt-8 grid gap-4">
          {addresses.map((address) => (
            <li key={address.id}>
              <AddressCard
                address={address}
                busy={saving}
                confirmingDeactivate={pendingDeactivateId === address.id}
                onEdit={() => setEditor({ mode: "edit", address })}
                onAskDeactivate={() => setPendingDeactivateId(address.id)}
                onCancelDeactivate={() => setPendingDeactivateId(null)}
                onDeactivate={() => {
                  deactivateMutation.mutate(address.id, {
                    onSuccess: () => setPendingDeactivateId(null),
                  });
                }}
                onSetDefault={() => defaultMutation.mutate(address.id)}
              />
            </li>
          ))}
        </ul>
      )}
    </Container>
  );
}

function Header() {
  return (
    <div>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Direcciones
      </h1>
      <p className="mt-2 max-w-2xl text-base text-sf-muted">
        Usa estas direcciones al realizar un pedido. Solo las direcciones
        activas están disponibles para checkout.
      </p>
    </div>
  );
}

function AddressCard({
  address,
  busy,
  confirmingDeactivate,
  onEdit,
  onAskDeactivate,
  onCancelDeactivate,
  onDeactivate,
  onSetDefault,
}: {
  address: Address;
  busy: boolean;
  confirmingDeactivate: boolean;
  onEdit: () => void;
  onAskDeactivate: () => void;
  onCancelDeactivate: () => void;
  onDeactivate: () => void;
  onSetDefault: () => void;
}) {
  const active = address.status === "ACTIVE";

  return (
    <Card className="grid gap-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-lg font-semibold text-sf-ink">{address.label}</p>
          <p className="mt-1 text-sm text-sf-muted">{address.recipientName}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge tone={active ? "primary" : "neutral"}>
            {active ? "Activa" : "Inactiva"}
          </Badge>
          {address.isDefault ? (
            <Badge tone="accent">Predeterminada</Badge>
          ) : null}
        </div>
      </div>
      <dl className="grid gap-2 text-sm">
        <Info label="Dirección" value={address.addressLine} />
        {address.additionalInfo ? (
          <Info label="Información adicional" value={address.additionalInfo} />
        ) : null}
        <Info label="Ciudad" value={address.city} />
        <Info label="Departamento" value={address.department} />
        <Info label="Teléfono" value={address.phone} />
      </dl>
      {active ? (
        confirmingDeactivate ? (
          <div className="grid gap-2">
            <p className="text-sm font-semibold text-sf-ink">
              ¿Desactivar esta dirección?
            </p>
            <p className="text-sm text-sf-muted">
              Una dirección inactiva no se puede usar para realizar un pedido.
            </p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                type="button"
                variant="destructive"
                className="flex-1"
                disabled={busy}
                onClick={onDeactivate}
              >
                {busy ? "Desactivando…" : "Sí, desactivar"}
              </Button>
              <Button
                type="button"
                variant="secondary"
                className="flex-1"
                disabled={busy}
                onClick={onCancelDeactivate}
              >
                Conservar
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              disabled={busy}
              onClick={onEdit}
            >
              Editar
            </Button>
            {address.isDefault ? null : (
              <Button
                type="button"
                variant="secondary"
                className="flex-1"
                disabled={busy}
                onClick={onSetDefault}
              >
                Predeterminada
              </Button>
            )}
            <Button
              type="button"
              variant="ghost"
              className="flex-1"
              disabled={busy}
              onClick={onAskDeactivate}
            >
              Desactivar
            </Button>
          </div>
        )
      ) : (
        <p className="text-sm text-sf-muted">
          Esta dirección está inactiva. No se puede editar, marcar como
          predeterminada ni usar en un pedido.
        </p>
      )}
    </Card>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid gap-0.5 sm:grid-cols-[10rem_minmax(0,1fr)]">
      <dt className="text-sf-muted">{label}</dt>
      <dd className="font-semibold text-sf-ink">{value}</dd>
    </div>
  );
}

function mutationMessage(error: unknown): string | null {
  if (!error) {
    return null;
  }
  if (isApiError(error)) {
    return messageForApiProblem(error.problem);
  }
  return "No se pudo completar la operación.";
}
