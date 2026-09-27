"use client";

import { useEffect, useState } from "react";
import {
  formatPreviewRemaining,
  previewRemainingMs,
  type PreviewMeta,
} from "@/shared/session/preview-session";
import { Button, buttonClassName } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

const FIVE_MINUTES_MS = 5 * 60_000;
const TWO_MINUTES_MS = 2 * 60_000;

function remainingToneClass(remainingMs: number): string {
  if (remainingMs > FIVE_MINUTES_MS) {
    return "text-sf-ink";
  }
  if (remainingMs >= TWO_MINUTES_MS) {
    return "text-amber-700";
  }
  return "text-red-600";
}

export function StorefrontPreviewBanner({
  previewExpiresAt,
  onReturnToAdmin,
  onExitPreview,
}: {
  previewExpiresAt: string;
  onReturnToAdmin: () => void;
  onExitPreview: () => void;
}) {
  const [remainingMs, setRemainingMs] = useState(() =>
    previewRemainingMs({
      previewId: "",
      previewExpiresAt,
      temporaryCustomerId: "",
    }),
  );

  useEffect(() => {
    const meta: PreviewMeta = {
      previewId: "",
      previewExpiresAt,
      temporaryCustomerId: "",
    };
    const update = () => setRemainingMs(previewRemainingMs(meta));
    update();
    const id = window.setInterval(update, 1000);
    return () => window.clearInterval(id);
  }, [previewExpiresAt]);

  if (remainingMs <= 0) {
    return null;
  }

  return (
    <div
      role="status"
      aria-live="polite"
      className="border-b border-amber-200/70 bg-amber-50 text-sf-ink"
    >
      <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-3 md:flex-row md:items-center md:justify-between md:px-8">
        <div className="min-w-0">
          <p className="font-semibold text-sf-ink">Modo de prueba</p>
          <p
            className={cx(
              "mt-1 text-sm font-semibold tabular-nums",
              remainingToneClass(remainingMs),
            )}
          >
            {formatPreviewRemaining(remainingMs)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2 md:shrink-0">
          <button
            type="button"
            onClick={onReturnToAdmin}
            className={buttonClassName(
              "secondary",
              cx(
                "border-emerald-200/90 bg-emerald-50 text-emerald-800",
                "hover:border-emerald-300 hover:bg-emerald-100 hover:text-emerald-900",
                "transition-colors duration-150",
              ),
            )}
          >
            Volver a administración
          </button>
          <Button type="button" variant="destructive" onClick={onExitPreview}>
            Salir del modo de prueba
          </Button>
        </div>
      </div>
    </div>
  );
}
