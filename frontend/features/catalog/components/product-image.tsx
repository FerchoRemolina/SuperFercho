"use client";

import { useState, type ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

export function ProductImage({
  src,
  alt,
  className,
  children,
}: {
  src: string | null | undefined;
  alt: string;
  className?: string;
  children?: ReactNode;
}) {
  const [failed, setFailed] = useState(false);
  const hasImage = Boolean(src && src.trim() && !failed);

  return (
    <div className={cx("relative aspect-square", className)}>
      <div className="flex h-full w-full items-center justify-center overflow-hidden rounded-xl bg-sf-bg">
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
      {children}
    </div>
  );
}
