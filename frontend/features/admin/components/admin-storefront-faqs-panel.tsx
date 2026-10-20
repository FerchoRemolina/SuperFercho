"use client";

import { useMemo, useState } from "react";
import {
  useAdminStorefrontFaqsQuery,
  useCreateAdminStorefrontFaqMutation,
  useDeleteAdminStorefrontFaqMutation,
  useReorderAdminStorefrontFaqsMutation,
  useUpdateAdminStorefrontFaqMutation,
} from "@/features/admin/hooks";
import type { AdminStorefrontFaq } from "@/features/admin/api";
import { ApiError } from "@/shared/api/client";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

const inputClass =
  "min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary";

type FaqDraft = {
  question: string;
  answer: string;
  active: boolean;
};

const EMPTY_DRAFT: FaqDraft = {
  question: "",
  answer: "",
  active: true,
};

function nextDisplayOrder(faqs: readonly AdminStorefrontFaq[]): number {
  if (faqs.length === 0) {
    return 10;
  }
  return Math.max(...faqs.map((faq) => faq.displayOrder)) + 10;
}

function mutationMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return messageForApiProblem(error.problem);
  }
  return "No se pudo completar la operación.";
}

export function AdminStorefrontFaqsPanel() {
  const faqsQuery = useAdminStorefrontFaqsQuery();
  const createMutation = useCreateAdminStorefrontFaqMutation();
  const updateMutation = useUpdateAdminStorefrontFaqMutation();
  const reorderMutation = useReorderAdminStorefrontFaqsMutation();
  const deleteMutation = useDeleteAdminStorefrontFaqMutation();

  const [createDraft, setCreateDraft] = useState<FaqDraft>(EMPTY_DRAFT);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editDraft, setEditDraft] = useState<FaqDraft>(EMPTY_DRAFT);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const faqs = useMemo(
    () =>
      [...(faqsQuery.data ?? [])].sort(
        (a, b) => a.displayOrder - b.displayOrder || a.id.localeCompare(b.id),
      ),
    [faqsQuery.data],
  );

  const busy =
    createMutation.isPending ||
    updateMutation.isPending ||
    reorderMutation.isPending ||
    deleteMutation.isPending;

  function startEdit(faq: AdminStorefrontFaq) {
    setPendingDeleteId(null);
    setEditingId(faq.id);
    setEditDraft({
      question: faq.question,
      answer: faq.answer,
      active: faq.active,
    });
    setFeedback(null);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditDraft(EMPTY_DRAFT);
  }

  function createFaq() {
    if (busy) return;
    const question = createDraft.question.trim();
    const answer = createDraft.answer.trim();
    if (!question || !answer) {
      setFeedback("La pregunta y la respuesta son obligatorias.");
      return;
    }
    createMutation.mutate(
      {
        question,
        answer,
        displayOrder: nextDisplayOrder(faqs),
        active: createDraft.active,
      },
      {
        onSuccess: () => {
          setCreateDraft(EMPTY_DRAFT);
          setFeedback("Pregunta creada.");
        },
        onError: (error) => setFeedback(mutationMessage(error)),
      },
    );
  }

  function saveEdit(faq: AdminStorefrontFaq) {
    if (busy || editingId !== faq.id) return;
    const question = editDraft.question.trim();
    const answer = editDraft.answer.trim();
    if (!question || !answer) {
      setFeedback("La pregunta y la respuesta son obligatorias.");
      return;
    }
    updateMutation.mutate(
      {
        faqId: faq.id,
        body: {
          question,
          answer,
          displayOrder: faq.displayOrder,
          active: editDraft.active,
        },
      },
      {
        onSuccess: () => {
          cancelEdit();
          setFeedback("Pregunta actualizada.");
        },
        onError: (error) => setFeedback(mutationMessage(error)),
      },
    );
  }

  function toggleActive(faq: AdminStorefrontFaq) {
    if (busy) return;
    updateMutation.mutate(
      {
        faqId: faq.id,
        body: {
          question: faq.question,
          answer: faq.answer,
          displayOrder: faq.displayOrder,
          active: !faq.active,
        },
      },
      {
        onSuccess: () =>
          setFeedback(
            faq.active ? "Pregunta desactivada." : "Pregunta activada.",
          ),
        onError: (error) => setFeedback(mutationMessage(error)),
      },
    );
  }

  function moveFaq(faqId: string, direction: -1 | 1) {
    if (busy) return;
    const index = faqs.findIndex((faq) => faq.id === faqId);
    const target = index + direction;
    if (index < 0 || target < 0 || target >= faqs.length) {
      return;
    }
    const ordered = faqs.map((faq) => faq.id);
    const [removed] = ordered.splice(index, 1);
    ordered.splice(target, 0, removed);
    reorderMutation.mutate(ordered, {
      onSuccess: () => setFeedback("Orden actualizado."),
      onError: (error) => setFeedback(mutationMessage(error)),
    });
  }

  function confirmDelete(faqId: string) {
    if (busy) return;
    deleteMutation.mutate(faqId, {
      onSuccess: () => {
        setPendingDeleteId(null);
        if (editingId === faqId) {
          cancelEdit();
        }
        setFeedback("Pregunta eliminada.");
      },
      onError: (error) => setFeedback(mutationMessage(error)),
    });
  }

  if (faqsQuery.isPending) {
    return (
      <Card className="mt-6 p-4 md:p-5">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="mt-4 h-28 w-full" />
        <Skeleton className="mt-3 h-28 w-full" />
      </Card>
    );
  }

  if (faqsQuery.isError) {
    return (
      <Card className="mt-6 p-4 md:p-5">
        <Alert tone="error" title="No se pudieron cargar las preguntas">
          {faqsQuery.error instanceof ApiError
            ? messageForApiProblem(faqsQuery.error.problem)
            : "Intenta de nuevo."}
        </Alert>
        <button
          type="button"
          className={cx(buttonClassName("secondary"), "mt-3 min-h-9 px-4 text-sm")}
          onClick={() => void faqsQuery.refetch()}
        >
          Reintentar
        </button>
      </Card>
    );
  }

  return (
    <div className="mt-6 grid gap-5">
      <Card className="p-4 md:p-5">
        <h2 className="text-lg font-semibold text-sf-ink">
          Preguntas frecuentes
        </h2>
        <p className="mt-1 text-sm text-sf-muted">
          Administra las preguntas publicadas en la tienda. Solo las activas
          aparecen para los clientes.
        </p>

        {feedback ? (
          <p className="mt-3 text-sm font-medium text-sf-ink" role="status">
            {feedback}
          </p>
        ) : null}

        <ul className="mt-5 grid gap-3">
          {faqs.map((faq, index) => {
            const editing = editingId === faq.id;
            const confirmingDelete = pendingDeleteId === faq.id;
            return (
              <li
                key={faq.id}
                className="rounded-xl border border-sf-border bg-sf-bg/40 p-4"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-semibold uppercase tracking-wide text-sf-muted">
                        Orden {faq.displayOrder}
                      </span>
                      <span
                        className={cx(
                          "rounded-full px-2 py-0.5 text-xs font-semibold",
                          faq.active
                            ? "bg-emerald-50 text-emerald-700"
                            : "bg-slate-100 text-slate-600",
                        )}
                      >
                        {faq.active ? "Activa" : "Inactiva"}
                      </span>
                    </div>
                    {editing ? (
                      <div className="mt-3 grid gap-3">
                        <label className="grid gap-1 text-sm">
                          <span className="font-semibold text-sf-ink">
                            Pregunta
                          </span>
                          <input
                            type="text"
                            value={editDraft.question}
                            disabled={busy}
                            maxLength={300}
                            onChange={(event) =>
                              setEditDraft((current) => ({
                                ...current,
                                question: event.target.value,
                              }))
                            }
                            className={inputClass}
                          />
                        </label>
                        <label className="grid gap-1 text-sm">
                          <span className="font-semibold text-sf-ink">
                            Respuesta
                          </span>
                          <textarea
                            rows={4}
                            value={editDraft.answer}
                            disabled={busy}
                            maxLength={2000}
                            onChange={(event) =>
                              setEditDraft((current) => ({
                                ...current,
                                answer: event.target.value,
                              }))
                            }
                            className={cx(inputClass, "min-h-[6rem] resize-y")}
                          />
                        </label>
                        <label className="inline-flex items-center gap-2 text-sm text-sf-ink">
                          <input
                            type="checkbox"
                            checked={editDraft.active}
                            disabled={busy}
                            onChange={(event) =>
                              setEditDraft((current) => ({
                                ...current,
                                active: event.target.checked,
                              }))
                            }
                          />
                          Publicada (activa)
                        </label>
                      </div>
                    ) : (
                      <>
                        <p className="mt-2 text-sm font-semibold text-sf-ink">
                          {faq.question}
                        </p>
                        <p className="mt-1 text-sm leading-relaxed text-sf-muted">
                          {faq.answer}
                        </p>
                      </>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      disabled={busy || index === 0}
                      className={cx(
                        buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                        (busy || index === 0) && "cursor-not-allowed opacity-70",
                      )}
                      onClick={() => moveFaq(faq.id, -1)}
                    >
                      Subir
                    </button>
                    <button
                      type="button"
                      disabled={busy || index === faqs.length - 1}
                      className={cx(
                        buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                        (busy || index === faqs.length - 1) &&
                          "cursor-not-allowed opacity-70",
                      )}
                      onClick={() => moveFaq(faq.id, 1)}
                    >
                      Bajar
                    </button>
                    {!editing ? (
                      <>
                        <button
                          type="button"
                          disabled={busy}
                          className={cx(
                            buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                            busy && "cursor-not-allowed opacity-70",
                          )}
                          onClick={() => startEdit(faq)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          className={cx(
                            buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                            busy && "cursor-not-allowed opacity-70",
                          )}
                          onClick={() => toggleActive(faq)}
                        >
                          {faq.active ? "Desactivar" : "Activar"}
                        </button>
                        {confirmingDelete ? (
                          <>
                            <button
                              type="button"
                              disabled={busy}
                              className={cx(
                                buttonClassName("primary", "min-h-9 px-3 text-sm"),
                                busy && "cursor-not-allowed opacity-70",
                              )}
                              onClick={() => confirmDelete(faq.id)}
                            >
                              Confirmar eliminación
                            </button>
                            <button
                              type="button"
                              disabled={busy}
                              className={cx(
                                buttonClassName(
                                  "secondary",
                                  "min-h-9 px-3 text-sm",
                                ),
                                busy && "cursor-not-allowed opacity-70",
                              )}
                              onClick={() => setPendingDeleteId(null)}
                            >
                              Cancelar
                            </button>
                          </>
                        ) : (
                          <button
                            type="button"
                            disabled={busy}
                            className={cx(
                              buttonClassName(
                                "secondary",
                                "min-h-9 px-3 text-sm",
                              ),
                              busy && "cursor-not-allowed opacity-70",
                            )}
                            onClick={() => {
                              setPendingDeleteId(faq.id);
                              setFeedback(null);
                            }}
                          >
                            Eliminar
                          </button>
                        )}
                      </>
                    ) : (
                      <>
                        <button
                          type="button"
                          disabled={busy}
                          className={cx(
                            buttonClassName("primary", "min-h-9 px-3 text-sm"),
                            busy && "cursor-not-allowed opacity-70",
                          )}
                          onClick={() => saveEdit(faq)}
                        >
                          Guardar
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          className={cx(
                            buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                            busy && "cursor-not-allowed opacity-70",
                          )}
                          onClick={cancelEdit}
                        >
                          Cancelar
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      </Card>

      <Card className="p-4 md:p-5">
        <h3 className="text-base font-semibold text-sf-ink">
          Nueva pregunta
        </h3>
        <div className="mt-4 grid gap-3">
          <label className="grid gap-1 text-sm">
            <span className="font-semibold text-sf-ink">Pregunta</span>
            <input
              type="text"
              value={createDraft.question}
              disabled={busy}
              maxLength={300}
              onChange={(event) =>
                setCreateDraft((current) => ({
                  ...current,
                  question: event.target.value,
                }))
              }
              className={inputClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="font-semibold text-sf-ink">Respuesta</span>
            <textarea
              rows={4}
              value={createDraft.answer}
              disabled={busy}
              maxLength={2000}
              onChange={(event) =>
                setCreateDraft((current) => ({
                  ...current,
                  answer: event.target.value,
                }))
              }
              className={cx(inputClass, "min-h-[6rem] resize-y")}
            />
          </label>
          <label className="inline-flex items-center gap-2 text-sm text-sf-ink">
            <input
              type="checkbox"
              checked={createDraft.active}
              disabled={busy}
              onChange={(event) =>
                setCreateDraft((current) => ({
                  ...current,
                  active: event.target.checked,
                }))
              }
            />
            Publicar al crear
          </label>
          <div>
            <button
              type="button"
              disabled={busy}
              className={cx(
                buttonClassName("primary", "min-h-9 px-4 text-sm"),
                busy && "cursor-not-allowed opacity-70",
              )}
              onClick={createFaq}
            >
              {createMutation.isPending ? "Creando…" : "Crear pregunta"}
            </button>
          </div>
        </div>
      </Card>
    </div>
  );
}
