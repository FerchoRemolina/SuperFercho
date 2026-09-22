"use client";

import { useState } from "react";
import type { KnowledgeDocument } from "@/features/admin/api";
import {
  useDeactivateAdminKnowledgeDocumentMutation,
  useProcessAdminKnowledgeDocumentMutation,
  useReactivateAdminKnowledgeDocumentMutation,
} from "@/features/admin/hooks";
import {
  canDeactivateKnowledgeDocument,
  canProcessKnowledgeDocument,
  canReactivateKnowledgeDocument,
  knowledgeDocumentDeactivateConfirmation,
  knowledgeDocumentProcessConfirmation,
  knowledgeDocumentReactivateConfirmation,
} from "@/features/admin/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";

type KnowledgeAction = "process" | "deactivate" | "reactivate";

function actionForStatus(
  status: KnowledgeDocument["status"],
): KnowledgeAction | null {
  if (canProcessKnowledgeDocument(status)) {
    return "process";
  }
  if (canDeactivateKnowledgeDocument(status)) {
    return "deactivate";
  }
  if (canReactivateKnowledgeDocument(status)) {
    return "reactivate";
  }
  return null;
}

export function KnowledgeDocumentStatusActions({
  document,
}: {
  document: KnowledgeDocument;
}) {
  const processMutation = useProcessAdminKnowledgeDocumentMutation();
  const deactivateMutation = useDeactivateAdminKnowledgeDocumentMutation();
  const reactivateMutation = useReactivateAdminKnowledgeDocumentMutation();
  const [confirming, setConfirming] = useState(false);

  const action = actionForStatus(document.status);
  const pending =
    processMutation.isPending ||
    deactivateMutation.isPending ||
    reactivateMutation.isPending;
  const error =
    processMutation.error ??
    deactivateMutation.error ??
    reactivateMutation.error;
  const errorMessage = error
    ? isApiError(error)
      ? messageForApiProblem(error.problem)
      : "No se pudo completar la solicitud."
    : null;

  if (action === null) {
    return (
      <p className="text-sm text-sf-muted">
        No hay acciones de estado disponibles para este documento.
      </p>
    );
  }

  const confirmation =
    action === "process"
      ? knowledgeDocumentProcessConfirmation({ title: document.title })
      : action === "deactivate"
        ? knowledgeDocumentDeactivateConfirmation({ title: document.title })
        : knowledgeDocumentReactivateConfirmation({ title: document.title });

  const idleLabel =
    action === "process"
      ? "Procesar"
      : action === "deactivate"
        ? "Desactivar"
        : "Reactivar";

  const confirmLabel =
    action === "process"
      ? "Sí, procesar"
      : action === "deactivate"
        ? "Sí, desactivar"
        : "Sí, reactivar";

  const pendingLabel =
    action === "process"
      ? "Procesando…"
      : action === "deactivate"
        ? "Desactivando…"
        : "Reactivando…";

  function resetMutations() {
    processMutation.reset();
    deactivateMutation.reset();
    reactivateMutation.reset();
  }

  function runAction() {
    const onSuccess = () => setConfirming(false);
    if (action === "process") {
      processMutation.mutate(document.id, { onSuccess });
      return;
    }
    if (action === "deactivate") {
      deactivateMutation.mutate(document.id, { onSuccess });
      return;
    }
    reactivateMutation.mutate(document.id, { onSuccess });
  }

  return (
    <div className="grid gap-3">
      {errorMessage ? (
        <Alert tone="error" title="No se pudo completar la acción">
          {errorMessage}
        </Alert>
      ) : null}

      {!confirming ? (
        <Button
          type="button"
          variant={action === "deactivate" ? "secondary" : "primary"}
          disabled={pending}
          onClick={() => {
            resetMutations();
            setConfirming(true);
          }}
        >
          {idleLabel}
        </Button>
      ) : (
        <div className="grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-3">
          <p className="text-sm font-semibold text-sf-ink">{confirmation.title}</p>
          <p className="text-sm text-sf-muted">{confirmation.body}</p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              type="button"
              variant={action === "deactivate" ? "destructive" : "primary"}
              disabled={pending}
              onClick={() => {
                if (pending) {
                  return;
                }
                runAction();
              }}
            >
              {pending ? pendingLabel : confirmLabel}
            </Button>
            <Button
              type="button"
              variant="secondary"
              disabled={pending}
              onClick={() => setConfirming(false)}
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
