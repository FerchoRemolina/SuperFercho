"use client";

import { useState, type ReactNode } from "react";
import Link from "next/link";
import { usePublicStorefrontFooterSettingsQuery } from "@/features/storefront/hooks";
import { normalizeSupportEmail } from "@/features/storefront/support-email";
import { SupportEmailActions } from "@/features/support/components/support-email-actions";
import { FooterWave } from "@/shared/ui/footer-wave";
import { brandingAssets } from "@/shared/branding/assets";
import { BrandLogo } from "@/shared/ui/brand-logo";
import { Container } from "@/shared/ui/container";
import {
  ChevronIcon,
  CreditCardIcon,
  DocumentIcon,
  GitHubIcon,
  GridIcon,
  HandshakeIcon,
  LeafIcon,
  LinkedInIcon,
  QuestionCircleIcon,
  UserIcon,
} from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

type FooterNavLink = {
  href: string;
  label: string;
  external?: boolean;
};

type FooterNavGroup = {
  id: string;
  title: string;
  Icon: typeof UserIcon;
  links: FooterNavLink[];
};

function buildNavGroups(): FooterNavGroup[] {
  return [
    {
      id: "tienda",
      title: "Tienda",
      Icon: GridIcon,
      links: [
        { href: "/", label: "Inicio" },
        { href: "/catalog", label: "Catálogo" },
        { href: "/search", label: "Buscar" },
      ],
    },
    {
      id: "cuenta",
      title: "Mi cuenta",
      Icon: UserIcon,
      links: [
        { href: "/orders", label: "Mis pedidos" },
        { href: "/favorites", label: "Favoritos" },
        { href: "/lists", label: "Mis listas" },
        { href: "/addresses", label: "Direcciones" },
      ],
    },
    {
      id: "legal",
      title: "Información legal",
      Icon: DocumentIcon,
      links: [
        { href: "/legal/privacy", label: "Política de privacidad" },
        { href: "/legal/terms", label: "Términos y condiciones" },
      ],
    },
    {
      id: "atencion",
      title: "Atención al cliente",
      Icon: QuestionCircleIcon,
      links: [
        { href: "/#preguntas-frecuentes", label: "Preguntas frecuentes" },
        { href: "/assistant", label: "Asistente Fercho" },
      ],
    },
  ];
}

const focusRing =
  "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#9fd29a]";

function FooterNavLinkItem({ link }: { link: FooterNavLink }) {
  const className = cx(
    "inline-flex min-h-10 items-center text-[0.9375rem] text-white/88 transition-colors duration-150",
    "hover:text-white hover:underline",
    focusRing,
  );

  if (link.external) {
    return (
      <a
        href={link.href}
        target="_blank"
        rel="noopener noreferrer"
        className={className}
      >
        {link.label}
      </a>
    );
  }

  return (
    <Link href={link.href} className={className}>
      {link.label}
    </Link>
  );
}

function FooterNavGroupColumn({
  group,
  open,
  onToggle,
  supportEmailSlot,
}: {
  group: FooterNavGroup;
  open: boolean;
  onToggle: () => void;
  supportEmailSlot?: ReactNode;
}) {
  const panelId = `footer-nav-${group.id}`;

  return (
    <nav
      aria-label={group.title}
      className={cx(
        "border-b border-white/10 last:border-b-0 md:border-b-0 md:border-l md:border-white/12 md:pl-5 md:first:border-l-0 md:first:pl-0 lg:pl-6 lg:first:pl-0",
        // Última columna (Atención): un poco más de aire a la derecha para correo + copiar.
        group.id === "atencion" && "lg:pr-3 xl:pr-4",
      )}
    >
      <p className="mb-3 hidden items-center gap-2 text-[0.7rem] font-bold uppercase tracking-[0.12em] text-[#c8ebc2] md:flex">
        <group.Icon
          aria-hidden="true"
          className="size-4 shrink-0 text-[#9fd29a]"
        />
        {group.title}
      </p>

      <button
        type="button"
        className={cx(
          "flex min-h-12 w-full items-center gap-2 py-3 text-left text-sm font-bold uppercase tracking-[0.1em] text-[#c8ebc2] md:hidden",
          focusRing,
        )}
        aria-expanded={open}
        aria-controls={panelId}
        onClick={onToggle}
      >
        <group.Icon
          aria-hidden="true"
          className="size-4 shrink-0 text-[#9fd29a]"
        />
        <span className="flex-1">{group.title}</span>
        <ChevronIcon
          aria-hidden="true"
          className={cx(
            "size-4 shrink-0 text-white/70 transition-transform duration-200",
            open ? "-rotate-90" : "rotate-90",
          )}
        />
      </button>

      <ul
        id={panelId}
        className={cx(
          "grid gap-0.5 pb-3 md:pb-0",
          open ? "grid" : "hidden md:grid",
        )}
      >
        {group.links.map((link) => (
          <li key={`${group.id}-${link.label}`}>
            <FooterNavLinkItem link={link} />
          </li>
        ))}
        {supportEmailSlot ? <li>{supportEmailSlot}</li> : null}
      </ul>
    </nav>
  );
}

export function SiteFooter() {
  const footerQuery = usePublicStorefrontFooterSettingsQuery();
  const settings = footerQuery.data;
  const year = new Date().getFullYear();
  const [openGroupId, setOpenGroupId] = useState<string | null>(null);

  const linkedinUrl = settings?.linkedinUrl?.trim() || null;
  const githubUrl = settings?.githubUrl?.trim() || null;
  const supportEmail = normalizeSupportEmail(settings?.supportEmail);
  const qrImageUrl = settings?.qrImageUrl ?? null;
  const footerReady = footerQuery.isSuccess;

  const navGroups = buildNavGroups();

  return (
    <footer className="relative mt-0">
      <div className="relative -mb-px bg-[#f7f4ec] text-[#06402b]">
        <FooterWave className="block h-14 w-full md:h-[4.25rem] lg:h-[5rem]" />
      </div>

      <div className="relative overflow-hidden bg-gradient-to-b from-[#06402b] via-[#0a4a32] to-[#053526] text-white">
        {/* Decoraciones de esquina detrás del contenido (PNG con alfa real). */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 z-0 select-none"
        >
          {/* eslint-disable-next-line @next/next/no-img-element -- branding /public */}
          <img
            src={brandingAssets.tomates}
            alt=""
            width={509}
            height={680}
            className="absolute bottom-0 left-0 w-[8.5rem] max-w-[40vw] origin-bottom-left -translate-x-[16%] translate-y-[12%] sm:w-[11.5rem] md:w-[13.5rem] lg:w-[15.5rem] lg:-translate-x-[12%] lg:translate-y-[10%] xl:w-[17rem]"
          />
          {/* eslint-disable-next-line @next/next/no-img-element -- branding /public */}
          <img
            src={brandingAssets.bolsaDeCompras}
            alt=""
            width={432}
            height={560}
            className="absolute bottom-0 right-0 w-[7.75rem] max-w-[36vw] origin-bottom-right translate-x-[16%] translate-y-[10%] sm:w-[10.5rem] md:w-[12.5rem] lg:w-[14.5rem] lg:translate-x-[12%] lg:translate-y-[8%] xl:w-[16rem]"
          />
        </div>

        <Container
          width="wide"
          className="relative z-10 grid gap-10 py-10 md:gap-12 md:py-12 lg:grid-cols-[minmax(15rem,19.5rem)_minmax(0,1fr)] lg:items-start lg:gap-10 lg:py-14 xl:gap-14"
        >
          <div className="grid content-start gap-3.5">
            <span className="inline-flex w-fit items-center rounded-2xl bg-white p-2.5 shadow-sm">
              <BrandLogo />
            </span>
            <p className="text-xl font-extrabold tracking-tight md:text-[1.4rem]">
              Tu súper, más cerca de ti.
            </p>
            <p className="max-w-sm text-sm leading-relaxed text-white/80">
              Organiza tu mercado en línea y encuentra todo lo que necesitas
              desde cualquier lugar.
            </p>

            {(linkedinUrl || githubUrl) && (
              <div className="flex items-center gap-2.5 pt-1">
                {linkedinUrl ? (
                  <a
                    href={linkedinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="LinkedIn de SuperFercho"
                    className={cx(
                      "inline-flex size-11 items-center justify-center rounded-full border border-white/30 text-white",
                      "transition-colors duration-150 hover:border-white/60 hover:bg-white/10",
                      focusRing,
                    )}
                  >
                    <LinkedInIcon className="size-5" />
                  </a>
                ) : null}
                {githubUrl ? (
                  <a
                    href={githubUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="GitHub de SuperFercho"
                    className={cx(
                      "inline-flex size-11 items-center justify-center rounded-full border border-white/30 text-white",
                      "transition-colors duration-150 hover:border-white/60 hover:bg-white/10",
                      focusRing,
                    )}
                  >
                    <GitHubIcon className="size-5" />
                  </a>
                ) : null}
              </div>
            )}

            <div className="mt-1 w-fit rounded-2xl border border-white/15 bg-white/[0.06] p-2">
              {qrImageUrl && linkedinUrl ? (
                <a
                  href={linkedinUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={cx(
                    "relative block overflow-hidden rounded-xl bg-white p-1.5 shadow-sm",
                    "transition-opacity duration-150 hover:opacity-90",
                    focusRing,
                  )}
                  aria-label="Abrir perfil de LinkedIn (código QR)"
                >
                  {/* eslint-disable-next-line @next/next/no-img-element -- QR desde administración */}
                  <img
                    src={qrImageUrl}
                    alt=""
                    width={96}
                    height={96}
                    className="size-24 object-contain"
                  />
                </a>
              ) : qrImageUrl ? (
                <div className="overflow-hidden rounded-xl bg-white p-1.5 shadow-sm">
                  {/* eslint-disable-next-line @next/next/no-img-element -- QR desde administración */}
                  <img
                    src={qrImageUrl}
                    alt="Código QR"
                    width={96}
                    height={96}
                    className="size-24 object-contain"
                  />
                </div>
              ) : (
                <div
                  aria-hidden="true"
                  className="flex size-24 items-center justify-center rounded-xl border border-dashed border-white/25 bg-white/5"
                >
                  <LinkedInIcon className="size-7 text-white/35" />
                </div>
              )}
            </div>
          </div>

          <div className="grid md:grid-cols-2 md:gap-x-2 md:gap-y-8 lg:grid-cols-4 lg:gap-0 lg:pr-10 xl:pr-14">
            {navGroups.map((group) => (
              <FooterNavGroupColumn
                key={group.id}
                group={group}
                open={openGroupId === group.id}
                onToggle={() =>
                  setOpenGroupId((current) =>
                    current === group.id ? null : group.id,
                  )
                }
                supportEmailSlot={
                  group.id === "atencion"
                    ? supportEmail
                      ? (
                          <SupportEmailActions
                            email={supportEmail}
                            variant="footer"
                          />
                        )
                      : footerReady
                        ? (
                            <span className="inline-flex min-h-10 items-center text-sm text-white/55">
                              Correo de soporte no configurado
                            </span>
                          )
                        : undefined
                    : undefined
                }
              />
            ))}
          </div>
        </Container>

        <div className="relative z-10 border-t border-white/15 bg-[#03281c]/55">
          <Container
            width="wide"
            className="grid gap-6 py-5 text-xs text-white/70 lg:grid-cols-3 lg:items-center lg:gap-8"
          >
            <div className="space-y-1 lg:pr-4">
              <p className="text-[0.8125rem] leading-relaxed text-white/80">
                Â© {year} SuperFercho. Todos los derechos reservados.
              </p>
              <p>Precios expresados en pesos colombianos (COP).</p>
            </div>

            <div className="lg:justify-self-center">
              <p className="mb-2.5 text-center text-[0.7rem] font-bold uppercase tracking-[0.1em] text-white/55">
                Métodos de pago disponibles
              </p>
              <div className="flex flex-wrap items-center justify-center gap-3">
                <span className="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-white/5 px-3 py-2 text-white/90">
                  <span
                    aria-hidden="true"
                    className="inline-flex size-8 items-center justify-center rounded-lg bg-white/10 text-[#b8e0b4]"
                  >
                    <CreditCardIcon className="size-4" />
                  </span>
                  Tarjeta (simulada)
                </span>
                <span className="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-white/5 px-3 py-2 text-white/90">
                  <span
                    aria-hidden="true"
                    className="inline-flex size-8 items-center justify-center rounded-lg bg-white/10 text-[#b8e0b4]"
                  >
                    <HandshakeIcon className="size-4" />
                  </span>
                  Pago contra entrega
                </span>
              </div>
            </div>

            <p className="inline-flex items-start gap-2 text-[0.8125rem] leading-relaxed text-white/75 lg:justify-self-end lg:text-right">
              <LeafIcon
                aria-hidden="true"
                className="mt-0.5 size-4 shrink-0 text-[#9fd29a] lg:order-2"
              />
              <span>
                Un proyecto en constante evolución, para ofrecerte una mejor
                experiencia.
              </span>
            </p>
          </Container>
        </div>
      </div>
    </footer>
  );
}
