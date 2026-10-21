"use client";

import { useEffect, useState } from "react";
import {
  formatSessionRemaining,
  remainingMsUntil,
  sessionCountdownTone,
} from "@/shared/session/session-countdown";
import { clearSession } from "@/shared/session/session";
import { useSession } from "@/shared/session/session-provider";
import { cx } from "@/shared/utils/cx";

/**
 * Compact JWT remaining time for normal CUSTOMER sessions only.
 * Hidden during storefront preview (preview banner owns that countdown).
 * At 00:00 clears the local session via clearSession (no popup, no HTTP).
 */
export function CustomerSessionCountdown() {
  const { session, isPreview } = useSession();
  const expiresAt =
    session?.role === "CUSTOMER" && !isPreview ? session.expiresAt : null;
  const [remainingMs, setRemainingMs] = useState(() =>
    expiresAt ? remainingMsUntil(expiresAt) : 0,
  );

  useEffect(() => {
    if (!expiresAt) {
      setRemainingMs(0);
      return;
    }
    const update = () => {
      const remaining = remainingMsUntil(expiresAt);
      setRemainingMs(remaining);
      if (remaining <= 0) {
        clearSession();
      }
    };
    update();
    const id = window.setInterval(update, 1000);
    return () => window.clearInterval(id);
  }, [expiresAt]);

  if (!expiresAt || remainingMs <= 0) {
    return null;
  }

  // Verde > 5 min · amarillo 2:00–5:00 · rojo < 2:00 (tiempo real restante).
  const tone = sessionCountdownTone(remainingMs);
  const toneClasses =
    tone === "green"
      ? "border-sf-primary/25 bg-sf-primary-soft text-sf-primary"
      : tone === "yellow"
        ? "border-sf-warning/30 bg-sf-yellow-soft text-sf-warning"
        : "border-sf-error/30 bg-red-50 text-sf-error";

  return (
    <p
      className={cx(
        "hidden min-w-[104px] items-center justify-center whitespace-nowrap rounded-full border px-3 py-1",
        "text-xs font-medium tabular-nums md:flex",
        toneClasses,
      )}
      title="Tiempo restante de sesión"
      aria-live="polite"
    >
      Sesión {formatSessionRemaining(remainingMs)}
    </p>
  );
}
