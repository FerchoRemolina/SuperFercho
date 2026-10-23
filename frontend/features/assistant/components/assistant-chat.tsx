"use client";

import { useEffect, useId, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  postAssistantChat,
  type ChatRestResponse,
  type PackageProposal,
} from "@/features/assistant/api";
import {
  confirmationChatBody,
  messageChatBody,
  packageProposalFromResponse,
  packageSelectionChatBody,
  pendingFromResponse,
} from "@/features/assistant/chat-request";
import { invalidateAfterAssistantChat } from "@/features/assistant/invalidate";
import {
  confirmationConfirmLabel,
  confirmationDescription,
  confirmationTitle,
  nextMessageId,
  packageOptionLabel,
  packageProposalTitle,
  type ChatThreadMessage,
  type PendingConfirmation,
} from "@/features/assistant/presentation";
import { isApiError, type ApiProblem } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";

/** La autoridad del vencimiento siempre es el backend (pausedUntil/remainingSeconds). */
type FerchoPause = {
  code: "FERCHO_PAUSED" | "FERCHO_SUSPENDED" | "FERCHO_UNAVAILABLE";
  message: string;
  /** Milisegundo absoluto (Date.now) estimado de vencimiento; null = sin dato. */
  expiresAtMs: number | null;
};

function pauseRemainingSeconds(
  pause: FerchoPause,
  nowMs: number,
): number | null {
  if (pause.code === "FERCHO_SUSPENDED" || pause.code === "FERCHO_UNAVAILABLE") {
    return null;
  }
  if (pause.expiresAtMs === null) {
    return null;
  }
  return Math.max(0, Math.ceil((pause.expiresAtMs - nowMs) / 1000));
}

function formatRemaining(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safe / 60);
  const seconds = safe % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

function ferchoPauseFromError(problem: ApiProblem): FerchoPause | null {
  if (
    problem.code !== "FERCHO_PAUSED" &&
    problem.code !== "FERCHO_SUSPENDED" &&
    problem.code !== "FERCHO_UNAVAILABLE"
  ) {
    return null;
  }
  const rawSeconds = problem.properties?.["remainingSeconds"];
  const pausedUntilRaw = problem.properties?.["pausedUntil"];
  const pausedUntilMs =
    typeof pausedUntilRaw === "string" ? Date.parse(pausedUntilRaw) : Number.NaN;
  const expiresAtMs = Number.isNaN(pausedUntilMs)
    ? typeof rawSeconds === "number" && Number.isFinite(rawSeconds) && rawSeconds >= 0
      ? Date.now() + rawSeconds * 1000
      : null
    : pausedUntilMs;
  return {
    code: problem.code,
    message: messageForApiProblem(problem),
    expiresAtMs,
  };
}

export function AssistantChat() {
  const queryClient = useQueryClient();
  const inputId = useId();
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatThreadMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [pendingConfirmation, setPendingConfirmation] =
    useState<PendingConfirmation | null>(null);
  const [packageProposal, setPackageProposal] =
    useState<PackageProposal | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [pause, setPause] = useState<FerchoPause | null>(null);
  const [nowTick, setNowTick] = useState(() => Date.now());

  const pauseSecondsLeft =
    pause === null
      ? null
      : pauseRemainingSeconds(pause, nowTick);

  useEffect(() => {
    if (pause === null || pauseSecondsLeft === null) {
      return;
    }
    const timer = window.setInterval(() => {
      setNowTick(Date.now());
    }, 1000);
    return () => window.clearInterval(timer);
  }, [pause, pauseSecondsLeft]);

  function resetConversation() {
    if (sending) {
      return;
    }
    setConversationId(null);
    setMessages([]);
    setDraft("");
    setPendingConfirmation(null);
    setPackageProposal(null);
    setError(null);
  }

  function applyAssistantResponse(
    response: ChatRestResponse,
    thread: ChatThreadMessage[],
  ) {
    setConversationId(response.conversationId);
    const withAssistant: ChatThreadMessage[] = [
      ...thread,
      {
        id: nextMessageId(thread, "assistant"),
        role: "assistant",
        content: response.assistantMessage,
      },
    ];
    setMessages(withAssistant);
    setPendingConfirmation(pendingFromResponse(response));
    setPackageProposal(packageProposalFromResponse(response));
    invalidateAfterAssistantChat(queryClient);
  }

  async function sendMessage(raw: string) {
    const message = raw.trim();
    if (!message || sending || pendingConfirmation) {
      return;
    }
    setError(null);
    setSending(true);
    const withUser: ChatThreadMessage[] = [
      ...messages,
      {
        id: nextMessageId(messages, "user"),
        role: "user",
        content: message,
      },
    ];
    setMessages(withUser);
    setDraft("");
    try {
      const response = await postAssistantChat(
        messageChatBody(conversationId, message),
      );
      applyAssistantResponse(response, withUser);
    } catch (caught) {
      if (isApiError(caught)) {
        const pause = ferchoPauseFromError(caught.problem);
        if (pause !== null) {
          setPause(pause);
          setError(pause.message);
          return;
        }
      }
      setError(
        isApiError(caught)
          ? messageForApiProblem(caught.problem)
          : "No se pudo enviar el mensaje.",
      );
    } finally {
      setSending(false);
    }
  }

  async function confirmPending() {
    if (!pendingConfirmation || !conversationId || sending) {
      return;
    }
    setError(null);
    setSending(true);
    try {
      const response = await postAssistantChat(
        confirmationChatBody(conversationId, pendingConfirmation.token),
      );
      applyAssistantResponse(response, messages);
    } catch (caught) {
      setError(
        isApiError(caught)
          ? messageForApiProblem(caught.problem)
          : "No se pudo confirmar la acción.",
      );
    } finally {
      setSending(false);
    }
  }

  function dismissConfirmation() {
    if (sending) {
      return;
    }
    setPendingConfirmation(null);
  }

  async function selectPackage(packageCount: number) {
    if (!packageProposal || !conversationId || sending || pendingConfirmation) {
      return;
    }
    setError(null);
    setSending(true);
    try {
      const response = await postAssistantChat(
        packageSelectionChatBody(
          conversationId,
          packageProposal.proposalId,
          packageCount,
        ),
      );
      applyAssistantResponse(response, messages);
    } catch (caught) {
      setError(
        isApiError(caught)
          ? messageForApiProblem(caught.problem)
          : "No se pudo agregar esa cantidad de paquetes.",
      );
    } finally {
      setSending(false);
    }
  }

  return (
    <Container as="main" className="flex flex-col gap-6 py-10 md:py-16">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            Fercho
          </h1>
          <p className="mt-2 max-w-2xl text-base text-sf-muted">
            Tu asistente de SuperFercho. Pregunta por productos, carrito,
            listas o pedidos.
          </p>
        </div>
        <Button
          type="button"
          variant="secondary"
          onClick={resetConversation}
          disabled={sending || (messages.length === 0 && !conversationId)}
        >
          Nueva conversación
        </Button>
      </div>

      <div className="flex min-h-[28rem] flex-col overflow-hidden rounded-xl border border-sf-border bg-sf-surface">
        <div
          className="flex flex-1 flex-col gap-3 overflow-y-auto p-4 md:p-6"
          aria-live="polite"
        >
          {messages.length === 0 && !sending ? (
            <p className="text-sm text-sf-muted">
              Escribe un mensaje para empezar a hablar con Fercho.
            </p>
          ) : null}
          {messages.map((message) => (
            <div
              key={message.id}
              className={
                message.role === "user"
                  ? "ml-auto max-w-[85%] rounded-xl bg-sf-primary px-4 py-3 text-sm text-white"
                  : "mr-auto max-w-[85%] rounded-xl bg-sf-bg px-4 py-3 text-sm text-sf-ink"
              }
            >
              <p className="mb-1 text-xs font-semibold opacity-80">
                {message.role === "user" ? "Tú" : "Fercho"}
              </p>
              <p className="whitespace-pre-wrap">{message.content}</p>
            </div>
          ))}
          {sending ? (
            <p className="text-sm font-semibold text-sf-muted">
              Fercho está pensando…
            </p>
          ) : null}
        </div>

        {pause ? (
          <div className="border-t border-sf-border px-4 py-3 md:px-6">
            <Alert
              tone={pause.code === "FERCHO_PAUSED" ? "warning" : "error"}
              title={
                pause.code === "FERCHO_SUSPENDED"
                  ? "Fercho suspendido temporalmente"
                  : pause.code === "FERCHO_UNAVAILABLE"
                    ? "Fercho no disponible"
                    : "Fercho está descansando"
              }
            >
              <p>{pause.message}</p>
              {pauseSecondsLeft !== null && pauseSecondsLeft > 0 ? (
                <p className="mt-1 text-sm font-semibold tabular-nums">
                  Podrás escribirme de nuevo en{" "}
                  {formatRemaining(pauseSecondsLeft)}.
                </p>
              ) : null}
              {pause.code !== "FERCHO_PAUSED" ? (
                <p className="mt-1 text-sm">
                  El resto de la tienda sigue funcionando con normalidad.
                </p>
              ) : null}
            </Alert>
          </div>
        ) : null}

        {error && pause === null ? (
          <div className="border-t border-sf-border px-4 py-3 md:px-6">
            <Alert tone="error" title="No se pudo completar">
              {error}
            </Alert>
          </div>
        ) : null}

        {packageProposal && !pendingConfirmation ? (
          <div className="border-t border-sf-border px-4 py-4 md:px-6">
            <p className="text-sm font-semibold text-sf-ink">
              {packageProposalTitle(packageProposal)}
            </p>
            <p className="mt-1 text-sm text-sf-muted">
              Elige un número de paquetes. Un «sí» por chat no agrega al
              carrito.
            </p>
            <div className="mt-3 flex flex-col gap-2 sm:flex-row sm:flex-wrap">
              {packageProposal.options.map((option) => (
                <Button
                  key={`${packageProposal.proposalId}-${option.packageCount}`}
                  type="button"
                  variant="secondary"
                  disabled={sending}
                  onClick={() => void selectPackage(option.packageCount)}
                >
                  {packageOptionLabel(option)}
                </Button>
              ))}
            </div>
          </div>
        ) : null}

        {pendingConfirmation ? (
          <div className="border-t border-sf-border px-4 py-4 md:px-6">
            <Alert
              tone="warning"
              title={confirmationTitle(pendingConfirmation.type)}
            >
              <p>{confirmationDescription(pendingConfirmation.type)}</p>
              <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                <Button
                  type="button"
                  variant="primary"
                  disabled={sending}
                  onClick={() => void confirmPending()}
                >
                  {confirmationConfirmLabel(pendingConfirmation.type)}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={sending}
                  onClick={dismissConfirmation}
                >
                  Cancelar
                </Button>
              </div>
            </Alert>
          </div>
        ) : null}

        <form
          className="flex flex-col gap-3 border-t border-sf-border p-4 md:flex-row md:items-end md:p-6"
          onSubmit={(event) => {
            event.preventDefault();
            void sendMessage(draft);
          }}
        >
          <div className="grid min-w-0 flex-1 gap-1">
            <label htmlFor={inputId} className="text-sm font-semibold">
              Mensaje
            </label>
            <input
              id={inputId}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              disabled={sending || pendingConfirmation !== null}
              placeholder="Escribe tu mensaje…"
              autoComplete="off"
              className="min-h-11 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink disabled:cursor-not-allowed disabled:bg-sf-bg"
            />
          </div>
          <Button
            type="submit"
            variant="primary"
            disabled={
              sending ||
              pendingConfirmation !== null ||
              (pause !== null && pauseSecondsLeft !== null && pauseSecondsLeft > 0) ||
              draft.trim().length === 0
            }
          >
            {sending ? "Enviando…" : "Enviar"}
          </Button>
        </form>
      </div>
    </Container>
  );
}
