"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { brandingAssets } from "@/shared/branding/assets";
import { useSession } from "@/shared/session/session-provider";
import { cx } from "@/shared/utils/cx";

/**
 * Asistente flotante permanente de Customer (Fercho), visible únicamente con
 * sesión CUSTOMER autenticada válida (el proveedor de sesión hidrata
 * sincrónicamente desde sessionStorage: sin parpadeo previo a la comprobación).
 * No se muestra donde el asistente ya es el contenido principal (/assistant)
 * ni sobre flujos enfocados con barra inferior (/checkout en móvil).
 */
export function FerchoFab() {
  const pathname = usePathname();
  const { session } = useSession();

  if (session?.role !== "CUSTOMER") {
    return null;
  }
  if (pathname.startsWith("/assistant") || pathname.startsWith("/checkout")) {
    return null;
  }

  return (
    <Link
      href="/assistant"
      aria-label="Chatear con Fercho, tu asistente de compras"
      title="Fercho, tu asistente"
      className={cx(
        "fixed bottom-5 right-4 z-30 flex size-14 items-center justify-center rounded-full",
        "border border-sf-border/80 bg-sf-surface shadow-[0_8px_24px_rgba(23,33,27,0.18)]",
        "transition-transform duration-200 hover:scale-105 active:scale-95",
        "motion-reduce:transition-none motion-reduce:hover:scale-100 motion-reduce:active:scale-100",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
        "md:bottom-6 md:right-6 md:size-16",
      )}
    >
      {/* eslint-disable-next-line @next/next/no-img-element -- asset de marca local aprobado (PNG), mismo patrón que BrandLogo */}
      <img
        src={brandingAssets.ferchoIcon}
        alt=""
        width={64}
        height={64}
        className="size-11 rounded-full object-cover md:size-12"
      />
    </Link>
  );
}
