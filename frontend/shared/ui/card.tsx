import type { ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

export function Card({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cx(
        "rounded-2xl border border-sf-border bg-sf-surface p-6 shadow-[0_1px_2px_rgba(23,33,27,0.06)]",
        className,
      )}
    >
      {children}
    </div>
  );
}
