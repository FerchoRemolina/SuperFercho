"use client";

import { useState } from "react";
import Link from "next/link";
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
import { cx } from "@/shared/utils/cx";

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
      <Container as="main" className="py-8 md:py-16">
        <Header />
        <AddressesSkeleton />
      </Container>
    );
  }

  if (addressesQuery.isError) {
    const message = isApiError(addressesQuery.error)
      ? messageForApiProblem(addressesQuery.error.problem)
      : "No se pudieron cargar las direcciones.";
    return (
      <Container as="main" className="py-8 md:py-16">
        <Header />
        <div className="mt-6 grid gap-4 md:mt-8">
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
  const hasAddresses = addresses.length > 0;
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
    <Container as="main" className="py-8 md:py-16">
      <Header />
      {mutationError && editor === null ? (
        <div className="mt-4 md:mt-5">
          <Alert tone="error" title="No se pudo actualizar">
            {mutationError}
          </Alert>
        </div>
      ) : null}

      {editor ? (
        <Card className="mt-6 grid gap-3 !p-4 md:mt-8 md:!p-5">
          <h2 className="text-lg font-semibold text-sf-ink md:text-xl">
            {editor.mode === "create" ? "Nueva dirección" : "Editar dirección"}
          </h2>
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
        </Card>
      ) : hasAddresses ? (
        <div className="mt-5 md:mt-6">
          <Button type="button" onClick={() => setEditor({ mode: "create" })}>
            Agregar dirección
          </Button>
        </div>
      ) : null}

      {!hasAddresses && editor === null ? (
        <div className="mt-6 md:mt-8">
          <EmptyState
            title="No tienes direcciones"
            description="Agrega tu primera dirección de entrega para pedir a domicilio."
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
      ) : hasAddresses ? (
        <ul className="mt-5 grid gap-3 md:mt-6 md:gap-4">
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
      ) : null}
    </Container>
  );
}

function Header() {
  return (
    <div>
      <Link
        href="/catalog"
        className="mb-3 inline-flex min-h-11 items-center text-sm font-semibold text-sf-muted hover:text-sf-primary md:mb-4"
      >
        ← Catálogo
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Direcciones de entrega
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Guarda dónde quieres recibir tus pedidos.
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
  const lineSecondary = [
    address.city,
    address.department,
  ]
    .filter(Boolean)
    .join(", ");

  return (
    <Card
      className={cx(
        "grid gap-3 !p-4 md:!p-4",
        !active && "bg-sf-bg/60 shadow-none",
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-base font-semibold tracking-tight text-sf-ink md:text-lg">
            {address.label}
          </p>
          <p className="mt-0.5 text-sm text-sf-muted">{address.recipientName}</p>
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          {address.isDefault ? (
            <Badge tone="accent" className="w-fit">
              Predeterminada
            </Badge>
          ) : null}
          {!active ? (
            <Badge tone="neutral" className="w-fit">
              Inactiva
            </Badge>
          ) : null}
        </div>
      </div>

      <div className="grid gap-0.5 text-sm">
        <p className="font-medium text-sf-ink">{address.addressLine}</p>
        {address.additionalInfo ? (
          <p className="text-sf-muted">{address.additionalInfo}</p>
        ) : null}
        <p className="text-sf-muted">
          {lineSecondary}
          {address.phone ? ` · ${address.phone}` : null}
        </p>
      </div>

      {active ? (
        confirmingDeactivate ? (
          <div className="grid gap-2 border-t border-sf-border pt-3">
            <p className="text-sm font-semibold text-sf-ink">
              ¿Desactivar esta dirección?
            </p>
            <p className="text-xs leading-relaxed text-sf-muted md:text-sm">
              La dirección no se eliminará. Solo dejará de estar disponible para
              nuevos pedidos.
            </p>
            <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
              <Button
                type="button"
                variant="destructive"
                disabled={busy}
                onClick={onDeactivate}
              >
                {busy ? "Desactivando…" : "Sí, desactivar"}
              </Button>
              <Button
                type="button"
                variant="secondary"
                disabled={busy}
                onClick={onCancelDeactivate}
              >
                Conservar
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 border-t border-sf-border pt-3">
            <Button
              type="button"
              variant="secondary"
              className="min-h-11"
              disabled={busy}
              onClick={onEdit}
            >
              Editar
            </Button>
            {address.isDefault ? null : (
              <Button
                type="button"
                variant="ghost"
                className="min-h-11 justify-start px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-primary"
                disabled={busy}
                onClick={onSetDefault}
              >
                Usar como dirección predeterminada
              </Button>
            )}
            <Button
              type="button"
              variant="ghost"
              className="min-h-11 justify-start px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
              disabled={busy}
              onClick={onAskDeactivate}
            >
              Desactivar
            </Button>
          </div>
        )
      ) : (
        <p className="border-t border-sf-border pt-3 text-xs text-sf-muted md:text-sm">
          Esta dirección está inactiva. No se puede editar, marcar como
          predeterminada ni usar en un pedido.
        </p>
      )}
    </Card>
  );
}

function AddressesSkeleton() {
  return (
    <ul className="mt-6 grid gap-3 md:mt-8 md:gap-4" aria-hidden="true">
      {Array.from({ length: 2 }, (_, index) => (
        <li key={index}>
          <Card className="grid gap-3 !p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <Skeleton className="h-5 w-1/3" />
                <Skeleton className="mt-2 h-4 w-1/4" />
              </div>
              <Skeleton className="h-7 w-28 rounded-lg" />
            </div>
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="mt-1 h-11 w-24" />
          </Card>
        </li>
      ))}
    </ul>
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
