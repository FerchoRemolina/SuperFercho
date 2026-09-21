import type { ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

export function EmptyState({
  title,
  description,
  action,
  className,
}: {
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cx(
        "flex flex-col items-start gap-3 rounded-2xl border border-dashed border-sf-border bg-sf-surface px-6 py-10",
        className,
      )}
    >
      <h2 className="text-xl font-semibold text-sf-ink md:text-2xl">{title}</h2>
      <p className="max-w-xl text-base text-sf-muted">{description}</p>
      {action}
    </div>
  );
}
