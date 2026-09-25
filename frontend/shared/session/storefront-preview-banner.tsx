"use client";

import { useEffect, useState } from "react";
import {
  formatPreviewRemaining,
  previewRemainingMs,
  type PreviewMeta,
} from "@/shared/session/preview-session";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

const TWO_MINUTES_MS = 2 * 60_000;

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

  const nearExpiry = remainingMs <= TWO_MINUTES_MS;

  return (
    <div
      role="status"
      aria-live="polite"
      className={cx(
        "border-b border-sf-warning/40 bg-amber-50 text-sf-ink",
        nearExpiry && "bg-amber-100",
      )}
    >
      <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-3 md:flex-row md:items-center md:justify-between md:px-8">
        <div className="min-w-0">
          <p className="font-semibold text-sf-warning">Modo de prueba</p>
          <p className="mt-1 text-sm text-sf-ink">
            Customer temporal ·{" "}
            <span className="font-semibold tabular-nums">
              {formatPreviewRemaining(remainingMs)}
            </span>
          </p>
        </div>
        <div className="flex flex-wrap gap-2 md:shrink-0">
          <Button type="button" variant="secondary" onClick={onReturnToAdmin}>
            Volver a administración
          </Button>
          <Button type="button" variant="destructive" onClick={onExitPreview}>
            Salir de preview
          </Button>
        </div>
      </div>
    </div>
  );
}
