"use client";

import { useState } from "react";
import { useSession } from "@/shared/session/session-provider";
import { Button } from "@/shared/ui/button";
import { ApiError } from "@/shared/errors/api-problem";

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
      <Button
        type="button"
        variant="primary"
        disabled={pending}
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
        {pending ? "Abriendo…" : "Ver tienda"}
      </Button>
      {error ? (
        <p className="mt-2 text-sm text-sf-error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
