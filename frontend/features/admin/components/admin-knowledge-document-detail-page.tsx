"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { AdminKnowledgeDocumentContentForm } from "@/features/admin/components/admin-knowledge-document-content-form";
import { KnowledgeDocumentStatusActions } from "@/features/admin/components/knowledge-document-status-actions";
import { KnowledgeDocumentStatusBadge } from "@/features/admin/components/knowledge-document-status-badge";
import {
  useAdminKnowledgeDocumentQuery,
  useReplaceAdminKnowledgeDocumentContentMutation,
} from "@/features/admin/hooks";
import {
  adminKnowledgeDocumentContentFormValuesFromDocument,
  type AdminKnowledgeDocumentContentFormValues,
  replaceAdminKnowledgeDocumentContentRequestFromValues,
  validateAdminKnowledgeDocumentContent,
} from "@/features/admin/payloads";
import {
  adminKnowledgeHref,
  formatAdminInstant,
} from "@/features/admin/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminKnowledgeDocumentDetailPage({
  documentId,
}: {
  documentId: string;
}) {
  const documentQuery = useAdminKnowledgeDocumentQuery(documentId);
  const replaceMutation = useReplaceAdminKnowledgeDocumentContentMutation();
  const [draftValues, setDraftValues] =
    useState<AdminKnowledgeDocumentContentFormValues | null>(null);
  const [replaced, setReplaced] = useState(false);

  const document = documentQuery.data;
  const values = useMemo(
    () =>
      draftValues ??
      (document
        ? adminKnowledgeDocumentContentFormValuesFromDocument({
            content: document.content,
          })
        : null),
    [document, draftValues],
  );

  const replaceError = replaceMutation.isError
    ? isApiError(replaceMutation.error)
      ? messageForApiProblem(replaceMutation.error.problem)
      : "No se pudo reemplazar el contenido."
    : null;

  if (documentQuery.isPending) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
        <Skeleton className="mt-4 h-64 w-full" />
      </Container>
    );
  }

  if (documentQuery.isError) {
    const queryError = documentQuery.error;
    const notFound =
      isApiError(queryError) && queryError.problem.code === "DOCUMENT_NOT_FOUND";
    return (
      <Container as="main" className="py-10 md:py-16">
        {notFound && isApiError(queryError) ? (
          <EmptyState
            title="No encontramos este documento"
            description={messageForApiProblem(queryError.problem)}
            action={
              <Link
                href={adminKnowledgeHref()}
                className={buttonClassName("secondary")}
              >
                Volver al listado
              </Link>
            }
          />
        ) : (
          <div className="grid gap-4">
            <Alert tone="error" title="No se pudo cargar el documento">
              {isApiError(queryError)
                ? messageForApiProblem(queryError.problem)
                : "No se pudo completar la solicitud."}
            </Alert>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Link
                href={adminKnowledgeHref()}
                className={buttonClassName("secondary")}
              >
                Volver al listado
              </Link>
              <Button
                type="button"
                variant="secondary"
                onClick={() => void documentQuery.refetch()}
              >
                Reintentar
              </Button>
            </div>
          </div>
        )}
      </Container>
    );
  }

  if (!document || !values) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-40 w-full" />
      </Container>
    );
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href={adminKnowledgeHref()}
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>

      {replaced ? (
        <div className="mb-6">
          <Alert tone="success" title="Contenido reemplazado">
            El documento está en estado Recibido. Debes procesarlo para
            indexarlo de nuevo.
          </Alert>
        </div>
      ) : null}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-sf-muted">Documento</p>
          <h1 className="mt-1 text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            {document.title}
          </h1>
          <p className="mt-2 text-base text-sf-muted">{document.source}</p>
        </div>
        <KnowledgeDocumentStatusBadge status={document.status} />
      </div>

      <div className="mt-8 grid gap-6">
        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Resumen</h2>
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-sf-muted">Creado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatAdminInstant(document.createdAt)}
              </dd>
            </div>
            <div>
              <dt className="text-sf-muted">Actualizado</dt>
              <dd className="mt-1 font-semibold text-sf-ink">
                {formatAdminInstant(document.updatedAt)}
              </dd>
            </div>
          </dl>
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Estado</h2>
          <KnowledgeDocumentStatusActions
            key={`${document.id}-${document.status}-${document.updatedAt}`}
            document={document}
          />
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Contenido</h2>
          <AdminKnowledgeDocumentContentForm
            key={document.updatedAt}
            values={values}
            pending={replaceMutation.isPending}
            error={replaceError}
            onChange={setDraftValues}
            validate={validateAdminKnowledgeDocumentContent}
            onSubmit={() => {
              replaceMutation.mutate(
                {
                  documentId: document.id,
                  body: replaceAdminKnowledgeDocumentContentRequestFromValues(
                    values,
                  ),
                },
                {
                  onSuccess: () => {
                    setDraftValues(null);
                    setReplaced(true);
                  },
                },
              );
            }}
          />
        </Card>

        <Card className="grid gap-4">
          <h2 className="text-xl font-semibold text-sf-ink">Fragmentos</h2>
          {document.chunks.length === 0 ? (
            <p className="text-sm text-sf-muted">
              Este documento todavía no tiene fragmentos. Procesa el documento
              para generarlos.
            </p>
          ) : (
            <ul className="grid gap-3">
              {document.chunks.map((chunk) => (
                <li
                  key={chunk.id}
                  className="grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-3"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-sf-ink">
                      Posición {chunk.position}
                    </p>
                    <p className="text-sm text-sf-muted">
                      {chunk.embedded ? "Indexado" : "Sin indexar"}
                    </p>
                  </div>
                  <p className="whitespace-pre-wrap text-sm text-sf-ink">
                    {chunk.text}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </Container>
  );
}
