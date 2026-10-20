"use client";

import { useEffect, useRef, useState } from "react";
import {
  MEDIA_IMAGE_ALLOWED_TYPES,
  validateMediaImage,
  type AdminStorefrontFooterSettings,
  type MediaImageValidationCode,
} from "@/features/admin/api";
import {
  useAdminStorefrontFooterSettingsQuery,
  useDeleteAdminStorefrontQrImageMutation,
  useUpdateAdminStorefrontFooterSettingsMutation,
  useUploadAdminStorefrontQrImageMutation,
} from "@/features/admin/hooks";
import { ApiError } from "@/shared/api/client";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

const MEDIA_IMAGE_ACCEPT = MEDIA_IMAGE_ALLOWED_TYPES.join(",");

const inputClass =
  "min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary";

type FooterFormState = {
  supportEmail: string;
  linkedinUrl: string;
  githubUrl: string;
};

function mediaImageValidationMessage(
  code: MediaImageValidationCode | "UPLOAD_FAILED",
): string {
  switch (code) {
    case "MEDIA_TOO_LARGE":
      return "La imagen supera el máximo de 2 MiB.";
    case "MEDIA_TYPE_NOT_SUPPORTED":
      return "Formato no soportado. Usa PNG, JPEG o WebP.";
    case "MEDIA_MISSING":
      return "Selecciona un archivo de imagen.";
    case "UPLOAD_FAILED":
      return "No se pudo subir la imagen del QR.";
  }
}

export type AdminStorefrontFooterPanelMode = "contact" | "qr" | "both";

function FooterForm({
  initial,
  mode = "both",
}: {
  initial: AdminStorefrontFooterSettings;
  mode?: AdminStorefrontFooterPanelMode;
}) {
  const updateMutation = useUpdateAdminStorefrontFooterSettingsMutation();
  const uploadQrMutation = useUploadAdminStorefrontQrImageMutation();
  const deleteQrMutation = useDeleteAdminStorefrontQrImageMutation();

  const [form, setForm] = useState<FooterFormState>({
    supportEmail: initial.supportEmail,
    linkedinUrl: initial.linkedinUrl,
    githubUrl: initial.githubUrl,
  });
  const [qrImageUrl, setQrImageUrl] = useState<string | null>(initial.qrImageUrl);
  const [localPreview, setLocalPreview] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [confirmDeleteQr, setConfirmDeleteQr] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const objectUrlRef = useRef<string | null>(null);

  const busy =
    updateMutation.isPending ||
    uploadQrMutation.isPending ||
    deleteQrMutation.isPending;

  useEffect(
    () => () => {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current);
      }
    },
    [],
  );

  function revokeLocalPreview() {
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }
    setLocalPreview(null);
  }

  function restoreFooter() {
    setForm({
      supportEmail: initial.supportEmail,
      linkedinUrl: initial.linkedinUrl,
      githubUrl: initial.githubUrl,
    });
    setFeedback(null);
  }

  function saveFooter() {
    if (busy) return;
    updateMutation.mutate(
      {
        supportEmail: form.supportEmail.trim(),
        linkedinUrl: form.linkedinUrl.trim(),
        githubUrl: form.githubUrl.trim(),
      },
      {
        onSuccess: (saved) => {
          setForm({
            supportEmail: saved.supportEmail,
            linkedinUrl: saved.linkedinUrl,
            githubUrl: saved.githubUrl,
          });
          setQrImageUrl(saved.qrImageUrl);
          setFeedback("Datos de contacto guardados.");
        },
        onError: (error) => {
          setFeedback(
            error instanceof ApiError
              ? messageForApiProblem(error.problem)
              : "No se pudieron guardar los datos.",
          );
        },
      },
    );
  }

  function onQrFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";
    if (!file || busy) return;

    const validationCode = validateMediaImage(file);
    if (validationCode) {
      revokeLocalPreview();
      setFeedback(mediaImageValidationMessage(validationCode));
      return;
    }

    const localUrl = URL.createObjectURL(file);
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
    }
    objectUrlRef.current = localUrl;
    setLocalPreview(localUrl);
    setConfirmDeleteQr(false);
    setFeedback(null);

    uploadQrMutation.mutate(file, {
      onSuccess: (result) => {
        revokeLocalPreview();
        setQrImageUrl(result.qrImageUrl);
        setFeedback("Imagen del QR guardada.");
      },
      onError: (error) => {
        revokeLocalPreview();
        if (error instanceof ApiError) {
          setFeedback(
            mediaImageValidationMessage(
              error.problem.code === "MEDIA_TOO_LARGE"
                ? "MEDIA_TOO_LARGE"
                : error.problem.code === "MEDIA_TYPE_NOT_SUPPORTED"
                  ? "MEDIA_TYPE_NOT_SUPPORTED"
                  : "UPLOAD_FAILED",
            ),
          );
          return;
        }
        setFeedback(mediaImageValidationMessage("UPLOAD_FAILED"));
      },
    });
  }

  function removeQr() {
    if (busy) return;
    deleteQrMutation.mutate(undefined, {
      onSuccess: (result) => {
        setQrImageUrl(result.qrImageUrl);
        setConfirmDeleteQr(false);
        setFeedback("Imagen del QR eliminada.");
      },
      onError: (error) => {
        setFeedback(
          error instanceof ApiError
            ? messageForApiProblem(error.problem)
            : "No se pudo eliminar la imagen del QR.",
        );
      },
    });
  }

  const previewSrc = localPreview ?? qrImageUrl;

  const showContact = mode === "contact" || mode === "both";
  const showQr = mode === "qr" || mode === "both";

  return (
    <div className="mt-6 grid gap-5">
      {showContact ? (
      <Card className="p-4 md:p-5">
        <h2 className="text-lg font-semibold text-sf-ink">
          Footer y contacto
        </h2>
        <p className="mt-1 text-sm text-sf-muted">
          Correo de soporte y enlaces sociales que verán los clientes en el
          pie de página.
        </p>

        <div className="mt-5 grid gap-4 md:max-w-xl">
          <label className="grid gap-1 text-sm">
            <span className="font-semibold text-sf-ink">Correo de soporte</span>
            <input
              type="email"
              value={form.supportEmail}
              disabled={busy}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  supportEmail: event.target.value,
                }))
              }
              className={inputClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="font-semibold text-sf-ink">URL de LinkedIn</span>
            <input
              type="url"
              value={form.linkedinUrl}
              disabled={busy}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  linkedinUrl: event.target.value,
                }))
              }
              className={inputClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="font-semibold text-sf-ink">URL de GitHub</span>
            <input
              type="url"
              value={form.githubUrl}
              disabled={busy}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  githubUrl: event.target.value,
                }))
              }
              className={inputClass}
            />
          </label>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-2">
          <button
            type="button"
            disabled={busy}
            className={cx(
              buttonClassName("primary", "min-h-9 px-4 text-sm"),
              busy && "cursor-not-allowed opacity-70",
            )}
            onClick={saveFooter}
          >
            {updateMutation.isPending ? "Guardando…" : "Guardar contacto"}
          </button>
          <button
            type="button"
            disabled={busy}
            className={cx(
              buttonClassName("secondary", "min-h-9 px-4 text-sm"),
              busy && "cursor-not-allowed opacity-70",
            )}
            onClick={restoreFooter}
          >
            Cancelar
          </button>
        </div>
      </Card>
      ) : null}

      {showQr ? (
      <Card className="p-4 md:p-5">
        <h2 className="text-lg font-semibold text-sf-ink">
          Código QR de LinkedIn
        </h2>
        <p className="mt-1 text-sm text-sf-muted">
          Adjunta una imagen PNG, JPEG o WebP (máximo 2 MiB). No se genera el
          QR automáticamente en esta etapa.
        </p>

        <div className="mt-5 flex max-w-sm items-center justify-center overflow-hidden rounded-xl border border-dashed border-sf-border bg-sf-bg/50 p-4">
          {previewSrc ? (
            /* eslint-disable-next-line @next/next/no-img-element -- preview del QR subido */
            <img
              src={previewSrc}
              alt="Vista previa del código QR de LinkedIn"
              className="h-auto max-h-64 w-full object-contain"
            />
          ) : (
            <p className="px-4 py-10 text-center text-sm text-sf-muted">
              Todavía no hay una imagen del QR. Adjunta un archivo para
              mostrarla en el pie de página.
            </p>
          )}
        </div>

        <input
          ref={fileInputRef}
          type="file"
          accept={MEDIA_IMAGE_ACCEPT}
          disabled={busy}
          className="hidden"
          onChange={onQrFileChange}
        />

        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            disabled={busy}
            className={cx(
              buttonClassName("secondary", "min-h-9 px-3 text-sm"),
              busy && "cursor-not-allowed opacity-70",
            )}
            onClick={() => fileInputRef.current?.click()}
          >
            {uploadQrMutation.isPending
              ? "Subiendo…"
              : qrImageUrl
                ? "Reemplazar imagen del QR"
                : "Adjuntar imagen del QR"}
          </button>
          {qrImageUrl ? (
            confirmDeleteQr ? (
              <>
                <button
                  type="button"
                  disabled={busy}
                  className={cx(
                    buttonClassName("primary", "min-h-9 px-3 text-sm"),
                    busy && "cursor-not-allowed opacity-70",
                  )}
                  onClick={removeQr}
                >
                  {deleteQrMutation.isPending
                    ? "Eliminando…"
                    : "Confirmar eliminación"}
                </button>
                <button
                  type="button"
                  disabled={busy}
                  className={cx(
                    buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                    busy && "cursor-not-allowed opacity-70",
                  )}
                  onClick={() => setConfirmDeleteQr(false)}
                >
                  Cancelar
                </button>
              </>
            ) : (
              <button
                type="button"
                disabled={busy}
                className={cx(
                  buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                  busy && "cursor-not-allowed opacity-70",
                )}
                onClick={() => setConfirmDeleteQr(true)}
              >
                Eliminar imagen
              </button>
            )
          ) : null}
        </div>
      </Card>
      ) : null}

      {feedback ? (
        <p className="text-sm font-medium text-sf-ink" role="status">
          {feedback}
        </p>
      ) : null}
    </div>
  );
}

export function AdminStorefrontFooterPanel({
  mode = "both",
}: {
  mode?: AdminStorefrontFooterPanelMode;
}) {
  const footerQuery = useAdminStorefrontFooterSettingsQuery();

  if (footerQuery.isPending) {
    return (
      <Card className="mt-6 p-4 md:p-5">
        <Skeleton className="h-6 w-52" />
        <Skeleton className="mt-4 h-40 w-full" />
      </Card>
    );
  }

  if (footerQuery.isError) {
    return (
      <Card className="mt-6 p-4 md:p-5">
        <Alert tone="error" title="No se pudo cargar la configuración del footer">
          {footerQuery.error instanceof ApiError
            ? messageForApiProblem(footerQuery.error.problem)
            : "Intenta de nuevo."}
        </Alert>
        <button
          type="button"
          className={cx(buttonClassName("secondary"), "mt-3 min-h-9 px-4 text-sm")}
          onClick={() => void footerQuery.refetch()}
        >
          Reintentar
        </button>
      </Card>
    );
  }

  return (
    <FooterForm
      key={`${footerQuery.data.supportEmail}|${footerQuery.data.linkedinUrl}|${footerQuery.data.githubUrl}|${footerQuery.data.qrImageUrl ?? ""}|${mode}`}
      initial={footerQuery.data}
      mode={mode}
    />
  );
}
