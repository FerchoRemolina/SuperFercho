"use client";

import { useEffect, useState } from "react";
import {
  formatSessionRemaining,
  remainingMsUntil,
} from "@/shared/session/session-countdown";
import { clearSession } from "@/shared/session/session";
import { useSession } from "@/shared/session/session-provider";

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

  return (
    <p
      className="hidden items-center text-xs font-medium tabular-nums text-sf-muted md:flex"
      title="Tiempo restante de sesión"
      aria-live="polite"
    >
      Sesión {formatSessionRemaining(remainingMs)}
    </p>
  );
}
