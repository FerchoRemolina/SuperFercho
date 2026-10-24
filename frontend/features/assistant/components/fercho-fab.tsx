"use client";

import { usePathname } from "next/navigation";
import { useFerchoShell } from "@/features/assistant/fercho-shell-context";
import { brandingAssets } from "@/shared/branding/assets";
import { useSession } from "@/shared/session/session-provider";
import { cx } from "@/shared/utils/cx";

/**
 * Asistente flotante permanente de Customer (Fercho), visible únicamente con
 * sesión CUSTOMER autenticada válida (el proveedor de sesión hidrata
 * sincrónicamente desde sessionStorage: sin parpadeo previo a la comprobación).
 * Abre y minimiza el panel; no navega a /assistant.
 * No se muestra donde el asistente ya es el contenido principal (/assistant)
 * ni sobre flujos enfocados con barra inferior (/checkout).
 */
export function FerchoFab() {
  const pathname = usePathname();
  const { session } = useSession();
  const { isOpen, toggle } = useFerchoShell();

  if (session?.role !== "CUSTOMER") {
    return null;
  }
  if (pathname.startsWith("/assistant") || pathname.startsWith("/checkout")) {
    return null;
  }

  return (
    <button
      type="button"
      aria-label={
        isOpen
          ? "Minimizar a Fercho"
          : "Chatear con Fercho, tu asistente de compras"
      }
      aria-expanded={isOpen}
      aria-controls="fercho-shell"
      title="Fercho, tu asistente"
      onClick={toggle}
      className={cx(
        "fixed bottom-5 right-4 z-30 flex size-14 items-center justify-center rounded-full",
        "border border-sf-border/80 bg-sf-surface shadow-[0_8px_24px_rgba(23,33,27,0.18)]",
        "transition-transform duration-200 hover:scale-105 active:scale-95",
        "motion-reduce:transition-none motion-reduce:hover:scale-100 motion-reduce:active:scale-100",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
        "md:bottom-6 md:right-6 md:size-16",
        isOpen && "max-md:hidden",
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
    </button>
  );
}
