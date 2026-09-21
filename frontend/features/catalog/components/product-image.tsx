"use client";

import { useState } from "react";
import { cx } from "@/shared/utils/cx";

export function ProductImage({
  src,
  alt,
  className,
}: {
  src: string | null | undefined;
  alt: string;
  className?: string;
}) {
  const [failed, setFailed] = useState(false);
  const hasImage = Boolean(src && src.trim() && !failed);

  return (
    <div
      className={cx(
        "flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-sf-bg",
        className,
      )}
    >
      {hasImage ? (
        // eslint-disable-next-line @next/next/no-img-element -- catalog imageUrl is an arbitrary backend URL
        <img
          src={src ?? ""}
          alt={alt}
          className="h-full w-full object-contain p-3"
          onError={() => setFailed(true)}
        />
      ) : (
        <span className="px-4 text-center text-sm text-sf-muted">Sin imagen</span>
      )}
    </div>
  );
}
