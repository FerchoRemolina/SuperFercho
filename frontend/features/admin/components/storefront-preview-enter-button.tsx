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
          "inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold",
          "bg-indigo-700 text-white transition-colors duration-150 hover:bg-indigo-800",
          "disabled:cursor-not-allowed disabled:bg-indigo-200 disabled:text-white/80",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-700",
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
