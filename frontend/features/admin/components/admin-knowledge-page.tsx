"use client";

import { type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { KnowledgeDocumentStatusBadge } from "@/features/admin/components/knowledge-document-status-badge";
import {
  useAdminKnowledgeDocumentsQuery,
  useAdminKnowledgeSearchQuery,
} from "@/features/admin/hooks";
import {
  adminKnowledgeDocumentHref,
  adminKnowledgeHref,
  adminKnowledgeNewHref,
  adminKnowledgeSearchQueryFromSearchParams,
  formatAdminInstant,
  formatKnowledgeSearchScore,
  shouldSearchAdminKnowledge,
} from "@/features/admin/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminKnowledgePageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const searchQuery = adminKnowledgeSearchQueryFromSearchParams({
    query: searchParams.get("query"),
    limit: searchParams.get("limit"),
  });
  const searching = shouldSearchAdminKnowledge(searchQuery.query);
  const documentsQuery = useAdminKnowledgeDocumentsQuery();
  const knowledgeSearchQuery = useAdminKnowledgeSearchQuery(searchQuery);

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submitted = String(
      new FormData(event.currentTarget).get("query") ?? "",
    ).trim();
    router.replace(
      adminKnowledgeHref({
        query: submitted,
        limit: searchQuery.limit,
      }),
    );
  }

  function clearSearch() {
    router.replace(adminKnowledgeHref());
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            Base de conocimiento
          </h1>
          <p className="mt-2 max-w-2xl text-base text-sf-muted">
            Consulta los documentos de la base de conocimiento del supermercado.
            La búsqueda semántica solo incluye documentos en estado Listo.
          </p>
        </div>
        <Link href={adminKnowledgeNewHref()} className={buttonClassName("primary")}>
          Nuevo documento
        </Link>
      </div>

      <form
        className="mt-8 grid gap-3 md:grid-cols-[1fr_auto]"
        role="search"
        onSubmit={onSearch}
      >
        <label htmlFor="admin-knowledge-search" className="sr-only">
          Buscar en la base de conocimiento
        </label>
        <input
          id="admin-knowledge-search"
          type="search"
          name="query"
          defaultValue={searchQuery.query}
          key={searchQuery.query}
          placeholder="Buscar en documentos listos"
          className="min-h-11 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink"
        />
        <div className="flex flex-col gap-2 sm:flex-row">
          <Button type="submit" className="flex-1">
            Buscar
          </Button>
          {searching ? (
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={clearSearch}
            >
              Limpiar
            </Button>
          ) : null}
        </div>
      </form>

      {searching ? (
        <section className="mt-8 grid gap-4" aria-labelledby="knowledge-search-heading">
          <div>
            <h2
              id="knowledge-search-heading"
              className="text-xl font-semibold text-sf-ink"
            >
              Resultados de búsqueda
            </h2>
            <p className="mt-1 text-sm text-sf-muted">
              Consulta: «{searchQuery.query}» · hasta {searchQuery.limit}{" "}
              resultados
            </p>
          </div>

          {knowledgeSearchQuery.isPending ? (
            <div className="grid gap-3" aria-hidden="true">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-28 w-full" />
            </div>
          ) : null}

          {knowledgeSearchQuery.isError ? (
            <div className="grid gap-4">
              <Alert tone="error" title="No se pudo completar la búsqueda">
                {isApiError(knowledgeSearchQuery.error)
                  ? messageForApiProblem(knowledgeSearchQuery.error.problem)
                  : "No se pudo completar la solicitud."}
              </Alert>
              <Button
                type="button"
                variant="secondary"
                onClick={() => void knowledgeSearchQuery.refetch()}
              >
                Reintentar
              </Button>
            </div>
          ) : null}

          {knowledgeSearchQuery.isSuccess &&
          knowledgeSearchQuery.data.hits.length === 0 ? (
            <EmptyState
              title="Sin resultados"
              description="No encontramos fragmentos listos para esta consulta."
            />
          ) : null}

          {knowledgeSearchQuery.isSuccess &&
          knowledgeSearchQuery.data.hits.length > 0 ? (
            <ul className="grid gap-4">
              {knowledgeSearchQuery.data.hits.map((hit) => (
                <li key={`${hit.documentId}-${hit.chunkId}`}>
                  <Card className="grid gap-3">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <p className="text-lg font-semibold text-sf-ink">
                          {hit.title}
                        </p>
                        <p className="mt-1 text-sm text-sf-muted">{hit.source}</p>
                      </div>
                      <p className="text-sm font-semibold text-sf-muted">
                        Puntuación {formatKnowledgeSearchScore(hit.score)}
                      </p>
                    </div>
                    <p className="whitespace-pre-wrap text-sm text-sf-ink">
                      {hit.chunkText}
                    </p>
                    <Link
                      href={adminKnowledgeDocumentHref(hit.documentId)}
                      className={buttonClassName("secondary")}
                    >
                      Ver documento
                    </Link>
                  </Card>
                </li>
              ))}
            </ul>
          ) : null}
        </section>
      ) : null}

      <section className="mt-10" aria-labelledby="knowledge-documents-heading">
        <h2
          id="knowledge-documents-heading"
          className="text-xl font-semibold text-sf-ink"
        >
          Documentos
        </h2>

        {documentsQuery.isPending ? (
          <div className="mt-4 grid gap-3" aria-hidden="true">
            <Skeleton className="h-28 w-full" />
            <Skeleton className="mt-0 h-28 w-full" />
          </div>
        ) : null}

        {documentsQuery.isError ? (
          <div className="mt-4 grid gap-4">
            <Alert tone="error" title="No se pudieron cargar los documentos">
              {isApiError(documentsQuery.error)
                ? messageForApiProblem(documentsQuery.error.problem)
                : "No se pudo completar la solicitud."}
            </Alert>
            <Button
              type="button"
              variant="secondary"
              onClick={() => void documentsQuery.refetch()}
            >
              Reintentar
            </Button>
          </div>
        ) : null}

        {documentsQuery.isSuccess && documentsQuery.data.length === 0 ? (
          <div className="mt-4">
            <EmptyState
              title="No hay documentos de conocimiento"
              description="Crea el primer documento para alimentar la base de conocimiento."
              action={
                <Link
                  href={adminKnowledgeNewHref()}
                  className={buttonClassName("primary")}
                >
                  Nuevo documento
                </Link>
              }
            />
          </div>
        ) : null}

        {documentsQuery.isSuccess && documentsQuery.data.length > 0 ? (
          <>
            <ul className="mt-4 grid gap-4 md:hidden">
              {documentsQuery.data.map((document) => (
                <li key={document.id}>
                  <Card className="grid gap-3">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-lg font-semibold text-sf-ink">
                          {document.title}
                        </p>
                        <p className="mt-1 text-sm text-sf-muted">
                          {document.source}
                        </p>
                      </div>
                      <KnowledgeDocumentStatusBadge status={document.status} />
                    </div>
                    <dl className="grid gap-2 text-sm">
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
                    <Link
                      href={adminKnowledgeDocumentHref(document.id)}
                      className={buttonClassName("secondary")}
                    >
                      Ver documento
                    </Link>
                  </Card>
                </li>
              ))}
            </ul>

            <div className="mt-4 hidden overflow-x-auto md:block">
              <table className="w-full min-w-[48rem] border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b border-sf-border text-sf-muted">
                    <th className="px-3 py-3 font-semibold">Título</th>
                    <th className="px-3 py-3 font-semibold">Fuente</th>
                    <th className="px-3 py-3 font-semibold">Estado</th>
                    <th className="px-3 py-3 font-semibold">Creado</th>
                    <th className="px-3 py-3 font-semibold">Actualizado</th>
                    <th className="px-3 py-3 font-semibold">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {documentsQuery.data.map((document) => (
                    <tr
                      key={document.id}
                      className="border-b border-sf-border align-top"
                    >
                      <td className="px-3 py-4 font-semibold text-sf-ink">
                        {document.title}
                      </td>
                      <td className="px-3 py-4 text-sf-muted">
                        {document.source}
                      </td>
                      <td className="px-3 py-4">
                        <KnowledgeDocumentStatusBadge status={document.status} />
                      </td>
                      <td className="px-3 py-4 text-sf-muted">
                        {formatAdminInstant(document.createdAt)}
                      </td>
                      <td className="px-3 py-4 text-sf-muted">
                        {formatAdminInstant(document.updatedAt)}
                      </td>
                      <td className="px-3 py-4">
                        <Link
                          href={adminKnowledgeDocumentHref(document.id)}
                          className={buttonClassName("secondary")}
                        >
                          Ver documento
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        ) : null}
      </section>
    </Container>
  );
}
