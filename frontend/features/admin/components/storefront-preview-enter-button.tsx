"use client";

import { useState } from "react";
import { useSession } from "@/shared/session/session-provider";
import { EyeIcon } from "@/shared/ui/icons";
import { ApiError } from "@/shared/errors/api-problem";
import { cx } from "@/shared/utils/cx";

export function StorefrontPreviewEnterButton({
  className,
}: {
  className?: string;
}) {
  const { session, enterStorefrontPreview } = useSession();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (session?.role !== "ADMIN") {
    return null;
  }

  return (
    <div className={className}>
      <button
        type="button"
        disabled={pending}
        className={cx(
          "inline-flex min-h-10 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold",
          "border border-sf-primary/25 bg-sf-primary/5 text-sf-primary",
          "shadow-[0_1px_2px_rgba(8,116,67,0.08)]",
          "transition-all duration-150 hover:-translate-y-px hover:border-sf-primary/45 hover:bg-sf-primary/10",
          "disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
        )}
        onClick={() => {
          setPending(true);
          setError(null);
          void enterStorefrontPreview()
            .catch((cause: unknown) => {
              if (cause instanceof ApiError) {
                setError(cause.problem.title ?? "No se pudo abrir la tienda");
                return;
              }
              setError("No se pudo abrir la tienda");
            })
            .finally(() => setPending(false));
        }}
      >
        <EyeIcon className="h-4 w-4 shrink-0" />
        {pending ? "Abriendo…" : "Ver tienda"}
      </button>
      {error ? (
        <p className="mt-2 text-sm text-sf-error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
