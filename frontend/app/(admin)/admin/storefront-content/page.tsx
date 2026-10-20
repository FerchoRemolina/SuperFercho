"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { AdminStorefrontFaqsPanel } from "@/features/admin/components/admin-storefront-faqs-panel";
import { AdminStorefrontFooterPanel } from "@/features/admin/components/admin-storefront-footer-panel";
import {
  useAdminStorefrontHeroQuery,
  useUpdateAdminStorefrontHeroMutation,
  useUploadAdminMediaImageMutation,
} from "@/features/admin/hooks";
import {
  MEDIA_IMAGE_ALLOWED_TYPES,
  validateMediaImage,
  type AdminStorefrontHero,
  type MediaImageValidationCode,
} from "@/features/admin/api";
import { ApiError } from "@/shared/api/client";
import { brandingAssets } from "@/shared/branding/assets";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { EyeIcon, SparklesIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

type HeroPresentationMode = "standard" | "immersive";
type StorefrontAdminSection = "hero" | "faqs" | "footer" | "qr";

const PRESENTATION_MODES: readonly {
  id: HeroPresentationMode;
  label: string;
}[] = [
  { id: "standard", label: "Estándar" },
  { id: "immersive", label: "Inmersivo" },
];

const SECTION_META: Record<
  StorefrontAdminSection,
  { title: string; description: string }
> = {
  hero: {
    title: "Hero",
    description: "Imagen y textos principales del inicio público.",
  },
  faqs: {
    title: "Preguntas frecuentes",
    description: "Preguntas y respuestas visibles en el inicio de la tienda.",
  },
  footer: {
    title: "Footer",
    description: "Correo de soporte y enlaces sociales del pie de página.",
  },
  qr: {
    title: "QR de LinkedIn",
    description: "Imagen del código QR mostrada en el footer público.",
  },
};

function resolveStorefrontSection(
  value: string | null,
): StorefrontAdminSection {
  if (value === "faqs" || value === "footer" || value === "qr" || value === "hero") {
    return value;
  }
  return "hero";
}

const MEDIA_IMAGE_ACCEPT = MEDIA_IMAGE_ALLOWED_TYPES.join(",");

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
      return "No se pudo subir la imagen.";
  }
}

const inputClass =
  "min-h-10 w-full rounded-lg border border-sf-border bg-sf-surface px-3 py-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary";

type HeroFormState = {
  title: string;
  description: string;
  imageUrl: string | null;
};

function Field({
  label,
  helper,
  children,
}: {
  label: string;
  helper?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <p className="text-sm font-semibold text-sf-ink">{label}</p>
      {helper ? <p className="mt-1 text-xs text-sf-muted">{helper}</p> : null}
      {children}
    </div>
  );
}

function SectionHeader({
  icon,
  title,
}: {
  icon: "layout" | "eye";
  title: string;
}) {
  const Icon = icon === "layout" ? SparklesIcon : EyeIcon;
  return (
    <div className="flex items-center gap-3">
      <span
        className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sf-primary/10 text-sf-primary"
        aria-hidden="true"
      >
        <Icon className="h-5 w-5" />
      </span>
      <div>
        <h2 className="text-lg font-semibold text-sf-ink">{title}</h2>
      </div>
    </div>
  );
}

export default function AdminStorefrontContentPage() {
  return (
    <Suspense
      fallback={
        <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="mt-4 h-64 w-full" />
        </main>
      }
    >
      <AdminStorefrontContentPageInner />
    </Suspense>
  );
}

function AdminStorefrontContentPageInner() {
  const searchParams = useSearchParams();
  const section = resolveStorefrontSection(searchParams.get("section"));
  const meta = SECTION_META[section];

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
        Contenido tienda
      </p>
      <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
        {meta.title}
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        {meta.description}
      </p>

      {section === "hero" ? <AdminStorefrontHeroSection /> : null}
      {section === "faqs" ? <AdminStorefrontFaqsPanel /> : null}
      {section === "footer" ? (
        <AdminStorefrontFooterPanel mode="contact" />
      ) : null}
      {section === "qr" ? <AdminStorefrontFooterPanel mode="qr" /> : null}
    </main>
  );
}

function AdminStorefrontHeroSection() {
  const heroQuery = useAdminStorefrontHeroQuery();

  if (heroQuery.isPending) {
    return (
      <div className="mt-6">
        <Skeleton className="h-80 w-full" />
        <Skeleton className="mt-5 h-64 w-full" />
      </div>
    );
  }

  if (heroQuery.isError) {
    const notConfigured =
      heroQuery.error instanceof ApiError &&
      heroQuery.error.problem.status === 404;
    return (
      <div className="mt-6 grid place-items-center gap-3 rounded-xl border border-sf-border bg-sf-surface p-10 text-center">
        <p className="text-sm text-sf-muted">
          {notConfigured
            ? "El contenido de la tienda aún no está configurado."
            : "No pudimos cargar el contenido de la tienda."}
        </p>
        <button
          type="button"
          onClick={() => void heroQuery.refetch()}
          className={cx(buttonClassName("secondary"), "min-h-9 px-4 text-sm")}
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <AdminStorefrontHeroForm
      key={heroQuery.data.updatedAt}
      initial={heroQuery.data}
    />
  );
}

function AdminStorefrontHeroForm({ initial }: { initial: AdminStorefrontHero }) {
  const updateMutation = useUpdateAdminStorefrontHeroMutation();
  const uploadMutation = useUploadAdminMediaImageMutation();

  const [hero, setHero] = useState<HeroFormState>({
    title: initial.title,
    description: initial.description,
    imageUrl: initial.imageUrl,
  });
  const [mode, setMode] = useState<HeroPresentationMode>(
    initial.mode === "IMMERSIVE" ? "immersive" : "standard",
  );
  const fileInputRef = useRef<HTMLInputElement>(null);
  const objectUrlRef = useRef<string | null>(null);
  const [localPreview, setLocalPreview] = useState<string | null>(null);
  const [uploadFeedback, setUploadFeedback] = useState<string | null>(null);

  const uploading = uploadMutation.isPending;
  const saving = updateMutation.isPending;
  const mutationError =
    updateMutation.error instanceof ApiError ? updateMutation.error : null;

  const imagePreviewSrc = localPreview ?? hero.imageUrl;

  const hasTitle = hero.title.trim() !== "";
  const hasDescription = hero.description.trim() !== "";

  const newImagePending =
    hero.imageUrl !== initial.imageUrl || localPreview !== null;

  useEffect(
    () => () => {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current);
        objectUrlRef.current = null;
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

  function restoreFromServer() {
    setHero({
      title: initial.title,
      description: initial.description,
      imageUrl: initial.imageUrl,
    });
    setMode(initial.mode === "IMMERSIVE" ? "immersive" : "standard");
    revokeLocalPreview();
    setUploadFeedback(null);
  }

  /** Quita la referencia de imagen del formulario. Nunca borra el archivo de
   *  MediaStorage: eso solo se refleja en el PUT cuando se guarda el Hero. */
  function removeImage() {
    revokeLocalPreview();
    setHero((current) => ({ ...current, imageUrl: null }));
    setUploadFeedback(null);
  }

  function onImageFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";
    if (!file) {
      return;
    }
    const validationCode = validateMediaImage(file);
    if (validationCode) {
      revokeLocalPreview();
      setUploadFeedback(mediaImageValidationMessage(validationCode));
      return;
    }
    setUploadFeedback(null);
    const localUrl = URL.createObjectURL(file);
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
    }
    objectUrlRef.current = localUrl;
    setLocalPreview(localUrl);
    uploadMutation.mutate(file, {
      onSuccess: (uploaded) => {
        revokeLocalPreview();
        setHero((current) => ({ ...current, imageUrl: uploaded.url }));
        setUploadFeedback("Imagen guardada correctamente.");
      },
      onError: (error) => {
        if (error instanceof ApiError) {
          setUploadFeedback(
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
        setUploadFeedback(mediaImageValidationMessage("UPLOAD_FAILED"));
      },
    });
  }

  function save() {
    if (saving || uploading) {
      return;
    }
    updateMutation.mutate({
      title: hero.title.trim(),
      description: hero.description.trim(),
      // El CTA independiente ya no existe; el contrato REST exige el campo.
      ctaLabel: "",
      imageUrl: hero.imageUrl,
      mode: mode === "immersive" ? "IMMERSIVE" : "STANDARD",
    });
  }

  return (
    <>
      <Card className="mt-6 p-4 md:p-5">
        <SectionHeader icon="layout" title="Contenido del Hero" />

        <div className="mt-5 grid gap-5 lg:grid-cols-2">
          <div className="grid content-start gap-5">
            <div>
              <p className="text-sm font-semibold text-sf-ink">
                Modo de presentación
              </p>
              <div
                className="mt-2 inline-flex rounded-lg border border-sf-border bg-sf-bg p-1"
                role="group"
                aria-label="Modo de presentación del hero"
              >
                {PRESENTATION_MODES.map((option) => {
                  const active = mode === option.id;
                  return (
                    <button
                      key={option.id}
                      type="button"
                      aria-pressed={active}
                      disabled={saving || uploading}
                      className={cx(
                        "min-h-8 rounded-md px-3 text-sm font-semibold transition-colors",
                        active
                          ? "bg-sf-surface text-sf-primary shadow-sm"
                          : "text-sf-muted hover:text-sf-ink",
                      )}
                      onClick={() => setMode(option.id)}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <Field label="Título">
              <input
                id="hero-title"
                type="text"
                value={hero.title}
                disabled={saving || uploading}
                onChange={(event) =>
                  setHero((current) => ({
                    ...current,
                    title: event.target.value,
                  }))
                }
                className={inputClass}
              />
            </Field>

            <Field label="Descripción">
              <textarea
                id="hero-description"
                rows={3}
                value={hero.description}
                disabled={saving || uploading}
                onChange={(event) =>
                  setHero((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
                className={cx(inputClass, "min-h-[5.5rem] resize-y")}
              />
            </Field>
          </div>

          <div className="grid content-start gap-3">
            <p className="text-sm font-semibold text-sf-ink">
              Imagen principal
            </p>
            <p className="-mt-2 text-xs text-sf-muted">
              Formatos permitidos: PNG, JPEG y WebP. Tamaño máximo: 2 MiB.
            </p>
            <div className="flex aspect-[21/9] w-full items-center justify-center overflow-hidden rounded-xl border border-sf-border bg-sf-surface">
              {imagePreviewSrc ? (
                /* eslint-disable-next-line @next/next/no-img-element -- preview de la imagen subida/seleccionada */
                <img
                  src={imagePreviewSrc}
                  alt=""
                  className="h-full w-full object-cover"
                />
              ) : (
                /* eslint-disable-next-line @next/next/no-img-element -- ilustración estática de branding desde /public */
                <img
                  src={brandingAssets.mark}
                  alt=""
                  className="h-auto w-full max-w-[14rem]"
                />
              )}
            </div>
            <input
              ref={fileInputRef}
              id="hero-image"
              type="file"
              accept={MEDIA_IMAGE_ACCEPT}
              disabled={saving || uploading}
              className="hidden"
              onChange={onImageFileChange}
            />
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                disabled={saving || uploading}
                onClick={() => fileInputRef.current?.click()}
                className={cx(
                  buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                  (saving || uploading) && "cursor-not-allowed opacity-70",
                )}
              >
                {uploading ? "Subiendo imagen…" : "Seleccionar imagen"}
              </button>
              {hero.imageUrl !== null || localPreview !== null ? (
                <button
                  type="button"
                  disabled={saving || uploading}
                  onClick={removeImage}
                  className={cx(
                    buttonClassName("secondary", "min-h-9 px-3 text-sm"),
                    (saving || uploading) && "cursor-not-allowed opacity-70",
                  )}
                >
                  {localPreview !== null || hero.imageUrl !== initial.imageUrl
                    ? "Quitar imagen nueva"
                    : "Quitar imagen"}
                </button>
              ) : null}
            </div>
            <p className="text-xs text-sf-muted">
              {uploading
                ? "Subiendo imagen…"
                : newImagePending && hero.imageUrl
                  ? "Imagen nueva lista: se guardará al pulsar Guardar cambios."
                  : hero.imageUrl
                    ? null
                    : "Sin imagen: se usará la ilustración de branding al guardar."}
            </p>
            {uploadFeedback ? (
              <p role="alert" className="text-xs font-semibold text-red-600">
                {uploadFeedback}
              </p>
            ) : null}
          </div>
        </div>
      </Card>

      <Card className="mt-5 p-4 md:p-5">
        <SectionHeader icon="eye" title="Vista previa" />

        <div className="relative mt-5 h-[22rem] w-full overflow-hidden rounded-xl border border-sf-border md:h-[26rem]">
          {mode === "immersive" ? (
            <>
              {imagePreviewSrc ? (
                /* eslint-disable-next-line @next/next/no-img-element -- preview de la imagen subida/seleccionada */
                <img
                  src={imagePreviewSrc}
                  alt=""
                  className="absolute inset-0 h-full w-full object-cover"
                />
              ) : (
                /* eslint-disable-next-line @next/next/no-img-element -- ilustración estática de branding desde /public */
                <img
                  src={brandingAssets.mark}
                  alt=""
                  className="absolute inset-0 h-full w-full bg-gradient-to-br from-sf-yellow-soft via-sf-bg to-sf-surface object-cover"
                />
              )}
              <div
                aria-hidden="true"
                className="absolute inset-0 bg-gradient-to-r from-sf-ink/85 via-sf-ink/45 to-transparent"
              />
              <div className="pointer-events-none relative flex h-full max-w-md flex-col justify-center gap-2.5 p-5 sm:p-6">
                {hasTitle ? (
                  <p className="text-2xl font-bold leading-tight tracking-tight text-white">
                    {hero.title}
                  </p>
                ) : null}
                {hasDescription ? (
                  <p className="text-sm leading-relaxed text-white/85">
                    {hero.description}
                  </p>
                ) : null}
              </div>
            </>
          ) : (
            <div className="grid h-full items-center gap-6 p-6 sm:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
              <div className="max-w-md">
                {hasTitle ? (
                  <p className="text-2xl font-bold leading-tight tracking-tight text-sf-ink">
                    {hero.title}
                  </p>
                ) : null}
                {hasDescription ? (
                  <p className="mt-3 text-sm leading-relaxed text-sf-muted">
                    {hero.description}
                  </p>
                ) : null}
              </div>
              <div className="flex h-full items-center justify-center overflow-hidden rounded-lg bg-sf-surface/70">
                {imagePreviewSrc ? (
                  /* eslint-disable-next-line @next/next/no-img-element -- preview de la imagen subida/seleccionada */
                  <img
                    src={imagePreviewSrc}
                    alt=""
                    className="h-full w-full object-cover"
                  />
                ) : (
                  /* eslint-disable-next-line @next/next/no-img-element -- ilustración estática de branding desde /public */
                  <img
                    src={brandingAssets.mark}
                    alt=""
                    className="h-auto w-2/3 max-w-[10rem]"
                  />
                )}
              </div>
            </div>
          )}
        </div>

        <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-sf-muted">
            {saving
              ? "Guardando cambios…"
              : uploading
                ? "Subiendo imagen…"
                : mutationError
                  ? `No se pudieron guardar los cambios${
                      mutationError.problem.code
                        ? ` (${mutationError.problem.code})`
                        : ""
                    }.`
                  : newImagePending
                    ? "Imagen nueva lista: se guardará al pulsar Guardar cambios."
                    : null}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={saving || uploading}
              className={cx(
                buttonClassName("secondary", "min-h-9 px-4 text-sm"),
                (saving || uploading) && "cursor-not-allowed opacity-70",
              )}
              onClick={restoreFromServer}
            >
              Cancelar
            </button>
            <button
              type="button"
              disabled={saving || uploading}
              className={cx(
                buttonClassName("primary", "min-h-9 px-4 text-sm"),
                (saving || uploading) && "cursor-not-allowed opacity-70",
              )}
              onClick={save}
            >
              {saving ? "Guardando…" : "Guardar cambios"}
            </button>
          </div>
        </div>
      </Card>
    </>
  );
}
