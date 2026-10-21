"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  usePublicStorefrontFaqsQuery,
  usePublicStorefrontFooterSettingsQuery,
} from "@/features/storefront/hooks";
import { normalizeSupportEmail } from "@/features/storefront/support-email";
import { FerchoSupportMascot } from "@/features/support/components/fercho-support-mascot";
import { SupportEmailActions } from "@/features/support/components/support-email-actions";
import { FaqLeafDecorations } from "@/features/support/components/faq-leaf-decorations";
import {
  ArrowRightIcon,
  CartIcon,
  ChatBubbleIcon,
  ChevronIcon,
  CreditCardIcon,
  HomeIcon,
  InfoIcon,
  MapPinIcon,
} from "@/shared/ui/icons";
import { Container } from "@/shared/ui/container";
import { cx } from "@/shared/utils/cx";

const FAQ_ICONS = [
  CartIcon,
  CreditCardIcon,
  MapPinIcon,
  InfoIcon,
  HomeIcon,
] as const;

const focusRing =
  "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0f6a3d]";

/** FAQ + soporte del inicio público (mascota: fercho-comprando.png). */
export function SupportSection() {
  const faqsQuery = usePublicStorefrontFaqsQuery();
  const footerQuery = usePublicStorefrontFooterSettingsQuery();
  const [openId, setOpenId] = useState<string | null>(null);

  const faqs = useMemo(
    () =>
      [...(faqsQuery.data ?? [])].sort(
        (a, b) => a.displayOrder - b.displayOrder || a.id.localeCompare(b.id),
      ),
    [faqsQuery.data],
  );

  const supportEmail = normalizeSupportEmail(footerQuery.data?.supportEmail);

  return (
    <section
      id="preguntas-frecuentes"
      aria-labelledby="support-heading"
      className="relative overflow-x-clip bg-[#f7f4ec] pt-14 md:pt-16 lg:pt-20"
    >
      <FaqLeafDecorations />

      <Container width="wide" className="relative z-10 pb-6 md:pb-8 lg:pb-10">
        <div className="mx-auto max-w-2xl text-center">
          <p className="inline-flex items-center gap-1.5 rounded-full bg-[#6faf6c] px-3.5 py-1.5 text-[0.68rem] font-bold uppercase tracking-[0.14em] text-white shadow-sm">
            <span
              aria-hidden="true"
              className="inline-flex size-4 items-center justify-center rounded-full bg-white/25 text-[0.65rem] font-black"
            >
              ?
            </span>
            Preguntas frecuentes
          </p>
          <h2
            id="support-heading"
            className="mt-4 text-[1.85rem] font-extrabold tracking-tight text-[#143528] md:text-[2.35rem] lg:text-[2.65rem]"
          >
            Â¿Tienes alguna duda?
            <span
              aria-hidden="true"
              className="ml-1.5 inline-block align-middle text-[#5ea85a]"
            >
              <svg viewBox="0 0 28 24" className="inline h-5 w-6 md:h-6 md:w-7">
                <path
                  d="M4 18c4-8 8-12 12-14"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.8"
                  strokeLinecap="round"
                />
                <path
                  d="M10 20c4-6 7-10 10-12"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.8"
                  strokeLinecap="round"
                />
                <path
                  d="M16 21c3-4 5-7 7-9"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.8"
                  strokeLinecap="round"
                />
              </svg>
            </span>
          </h2>
        </div>

        <div className="mt-10 grid items-start gap-10 lg:mt-12 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] lg:gap-10 xl:gap-12">
          <div className="grid gap-3">
            {faqsQuery.isPending ? (
              Array.from({ length: 5 }).map((_, index) => (
                <div
                  key={index}
                  className="h-14 animate-pulse rounded-[1.15rem] bg-white/80"
                />
              ))
            ) : faqsQuery.isError ? (
              <div className="rounded-[1.15rem] border border-sf-border bg-white p-5 text-sm text-sf-muted">
                No pudimos cargar las preguntas frecuentes. Intenta de nuevo más
                tarde.
              </div>
            ) : faqs.length === 0 ? (
              <div className="rounded-[1.15rem] border border-sf-border bg-white p-5 text-sm text-sf-muted">
                Pronto publicaremos preguntas frecuentes.
              </div>
            ) : (
              faqs.map((faq, index) => {
                const open = openId === faq.id;
                const Icon = FAQ_ICONS[index % FAQ_ICONS.length];
                const panelId = `faq-panel-${faq.id}`;
                return (
                  <div
                    key={faq.id}
                    className="overflow-hidden rounded-[1.15rem] border border-[#e6ebe3] bg-white shadow-[0_6px_20px_rgba(20,53,40,0.07)]"
                  >
                    <button
                      type="button"
                      aria-expanded={open}
                      aria-controls={panelId}
                      id={`faq-trigger-${faq.id}`}
                      onClick={() => setOpenId(open ? null : faq.id)}
                      className={cx(
                        "flex min-h-14 w-full items-center gap-3.5 px-4 py-3.5 text-left",
                        "transition-colors duration-150 hover:bg-[#f7faf5]",
                        focusRing,
                      )}
                    >
                      <span
                        aria-hidden="true"
                        className="inline-flex size-10 shrink-0 items-center justify-center rounded-full border border-[#cfe6c8] bg-[#e8f5e3] text-[#1a6b3a]"
                      >
                        <Icon className="size-[1.15rem]" />
                      </span>
                      <span className="min-w-0 flex-1 text-[0.9375rem] font-semibold leading-snug text-[#1a3228]">
                        {faq.question}
                      </span>
                      <ChevronIcon
                        aria-hidden="true"
                        className={cx(
                          "size-4 shrink-0 text-[#7a9084] transition-transform duration-200",
                          open ? "-rotate-90" : "rotate-90",
                        )}
                      />
                    </button>
                    {open ? (
                      <div
                        id={panelId}
                        role="region"
                        aria-labelledby={`faq-trigger-${faq.id}`}
                        className="border-t border-[#eef2ea] px-4 pb-4 pt-3 text-sm leading-relaxed text-[#5a7166] md:pl-[4.35rem]"
                      >
                        {faq.answer}
                      </div>
                    ) : null}
                  </div>
                );
              })
            )}
          </div>

          <aside className="relative">
            <h3 className="text-[1.35rem] font-extrabold tracking-tight text-[#143528] md:text-[1.6rem]">
              Estamos para ayudarte
            </h3>

            {/*
              Tarjeta mint: botones a la izquierda y Fercho a la derecha,
              centrado verticalmente respecto al bloque de las dos tarjetas.
            */}
            <div className="relative mt-5 w-full overflow-visible">
              <div
                className={cx(
                  "relative w-full overflow-visible rounded-[1.75rem]",
                  "bg-gradient-to-br from-[#d8ebcf] via-[#e6f1df] to-[#f4efe6]",
                  "shadow-[0_16px_40px_rgba(20,53,40,0.1)]",
                )}
              >
                <div className="relative flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:gap-1 sm:pr-3 md:gap-2 md:p-6 md:pr-4">
                  <div className="relative z-10 grid w-full max-w-[19.5rem] shrink-0 gap-3 sm:max-w-[16.75rem] md:max-w-[17.5rem] lg:max-w-[17.25rem] xl:max-w-[18rem]">
                    <Link
                      href="/assistant"
                      className={cx(
                        "group flex items-center gap-3 rounded-2xl bg-[#0d5c36] px-4 py-4 text-white shadow-[0_10px_28px_rgba(13,92,54,0.28)]",
                        "transition-[background-color,box-shadow] duration-150",
                        "hover:bg-[#0a4c2c] hover:shadow-[0_12px_32px_rgba(13,92,54,0.35)]",
                        focusRing,
                      )}
                    >
                      <span
                        aria-hidden="true"
                        className="inline-flex size-10 shrink-0 items-center justify-center rounded-full bg-white/15"
                      >
                        <ChatBubbleIcon className="size-5" />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-bold leading-tight">
                          Hablar con Fercho
                        </span>
                        <span className="mt-1 block text-[0.7rem] leading-snug text-white/85">
                          Pregúntale sobre productos, tu pedido o tu cuenta.
                        </span>
                      </span>
                      <ArrowRightIcon
                        aria-hidden="true"
                        className="size-4 shrink-0 transition-transform duration-150 group-hover:translate-x-0.5"
                      />
                    </Link>

                    {supportEmail ? (
                      <SupportEmailActions
                        email={supportEmail}
                        variant="card"
                      />
                    ) : (
                      <div className="rounded-2xl border border-dashed border-[#b7d1ae] bg-white/80 px-4 py-3.5 text-sm text-[#5d7268]">
                        El correo de soporte se mostrará cuando esté configurado.
                      </div>
                    )}
                  </div>

                  <div
                    aria-hidden="true"
                    className="pointer-events-none relative z-0 flex justify-center overflow-hidden sm:min-w-0 sm:flex-1 sm:justify-end sm:self-center sm:overflow-visible lg:overflow-visible"
                  >
                    {/* Desktop ~70px a la derecha; tablet/móvil con offset menor para no salirse. */}
                    <FerchoSupportMascot className="relative left-0 sm:left-3 md:left-5 lg:left-12 xl:left-[70px]" />
                  </div>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </Container>
    </section>
  );
}
