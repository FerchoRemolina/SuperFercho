"use client";

import { useEffect, useId, useRef, useState } from "react";
import { copyTextToClipboard } from "@/features/storefront/support-email";
import { CheckIcon, CopyIcon, MailIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

type SupportEmailActionsProps = {
  email: string;
  /** Estilo de la tarjeta de soporte (FAQ) o bloque del footer. */
  variant: "card" | "footer";
  className?: string;
};

/**
 * Muestra el correo de soporte como información de contacto (sin mailto)
 * y un botón para copiarlo al portapapeles.
 */
export function SupportEmailActions({
  email,
  variant,
  className,
}: SupportEmailActionsProps) {
  const statusId = useId();
  const [status, setStatus] = useState<"idle" | "copied" | "error">("idle");
  const resetTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (resetTimer.current) {
        clearTimeout(resetTimer.current);
      }
    };
  }, []);

  async function handleCopy() {
    const ok = await copyTextToClipboard(email);
    setStatus(ok ? "copied" : "error");
    if (resetTimer.current) {
      clearTimeout(resetTimer.current);
    }
    resetTimer.current = setTimeout(() => setStatus("idle"), 2500);
  }

  const statusMessage =
    status === "copied"
      ? "¡Correo copiado!"
      : status === "error"
        ? "No se pudo copiar el correo."
        : "";

  const copyAriaLabel =
    status === "copied"
      ? "Correo copiado"
      : status === "error"
        ? "Error al copiar el correo"
        : "Copiar correo de soporte";

  if (variant === "footer") {
    return (
      <div className={cx("grid gap-1", className)}>
        <div className="inline-flex max-w-full flex-nowrap items-center gap-1.5">
          <span className="whitespace-nowrap text-sm text-white/80">{email}</span>
          <button
            type="button"
            onClick={handleCopy}
            aria-label={copyAriaLabel}
            aria-describedby={statusId}
            className={cx(
              "inline-flex size-8 shrink-0 items-center justify-center rounded-md",
              "transition-colors duration-150 hover:bg-white/10",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#9fd29a]",
              status === "error"
                ? "text-[#ffc9c9]"
                : status === "copied"
                  ? "text-[#c8ebc2]"
                  : "text-[#b8e0b4]",
            )}
          >
            {status === "copied" ? (
              <CheckIcon className="size-3.5" aria-hidden="true" />
            ) : (
              <CopyIcon className="size-3.5" aria-hidden="true" />
            )}
          </button>
        </div>
        <p
          id={statusId}
          role="status"
          aria-live="polite"
          className={cx(
            "text-xs",
            status === "idle" ? "sr-only" : "",
            status === "error" ? "text-[#ffc9c9]" : "text-[#c8ebc2]",
          )}
        >
          {statusMessage || "\u00a0"}
        </p>
      </div>
    );
  }

  return (
    <div
      className={cx(
        "flex items-center gap-2 rounded-2xl border border-[#c5d9bc] bg-[#f3f7ef] px-3 py-4 text-[#143528] shadow-sm",
        className,
      )}
    >
      <span
        aria-hidden="true"
        className="inline-flex size-10 shrink-0 items-center justify-center rounded-full bg-[#dff0d8] text-[#0f6a3d]"
      >
        <MailIcon className="size-5" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-bold">Correo de soporte</p>
        <div className="mt-0.5 flex min-w-0 flex-nowrap items-center gap-1">
          <span
            className="min-w-0 truncate text-xs text-[#4f6a5c]"
            title={email}
          >
            {email}
          </span>
          <button
            type="button"
            onClick={handleCopy}
            aria-label={copyAriaLabel}
            aria-describedby={statusId}
            className={cx(
              "inline-flex size-7 shrink-0 items-center justify-center rounded-md",
              "transition-colors duration-150",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0f6a3d]",
              status === "error"
                ? "text-[#9b2c2c] hover:bg-[#fde8e8]"
                : status === "copied"
                  ? "text-[#0f6a3d] hover:bg-[#e8f4e3]"
                  : "text-[#5d7268] hover:bg-[#e8f4e3] hover:text-[#0f6a3d]",
            )}
          >
            {status === "copied" ? (
              <CheckIcon className="size-3.5" aria-hidden="true" />
            ) : (
              <CopyIcon className="size-3.5" aria-hidden="true" />
            )}
          </button>
        </div>
        <p
          id={statusId}
          role="status"
          aria-live="polite"
          className={cx(
            "mt-1 text-[0.7rem] font-medium leading-tight",
            status === "idle" ? "sr-only" : "",
            status === "error" ? "text-[#9b2c2c]" : "text-[#0f6a3d]",
          )}
        >
          {statusMessage || "\u00a0"}
        </p>
      </div>
    </div>
  );
}
