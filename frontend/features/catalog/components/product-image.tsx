"use client";

import { useState, type ReactNode } from "react";
import { brandingAssets } from "@/shared/branding/assets";
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
      <div
        className={cx(
          "flex h-full w-full items-center justify-center overflow-hidden rounded-xl",
          hasImage ? "bg-sf-bg" : "bg-gradient-to-br from-sf-bg via-sf-yellow-soft/40 to-sf-bg",
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
          <div className="flex flex-col items-center gap-2 px-4 text-center">
            {/* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */}
            <img
              src={brandingAssets.mark}
              alt=""
              width={48}
              height={48}
              className="size-12 opacity-90"
            />
            <span className="text-xs font-medium text-sf-muted">Sin imagen</span>
          </div>
        )}
      </div>
      {children}
    </div>
  );
}
