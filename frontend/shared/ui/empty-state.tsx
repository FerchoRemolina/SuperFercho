import type { ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

export function EmptyState({
  title,
  description,
  action,
  className,
  icon,
  align = "start",
}: {
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
  icon?: ReactNode;
  align?: "start" | "center";
}) {
  return (
    <div
      className={cx(
        "flex flex-col gap-3 rounded-2xl border border-dashed border-sf-border bg-sf-surface px-6 py-10",
        align === "center" ? "items-center text-center" : "items-start",
        className,
      )}
    >
      {icon ? (
        <span
          aria-hidden="true"
          className={cx(
            "flex size-12 items-center justify-center rounded-full bg-sf-primary-soft text-sf-primary",
          )}
        >
          {icon}
        </span>
      ) : null}
      <h2 className="text-xl font-semibold text-sf-ink md:text-2xl">{title}</h2>
      <p
        className={cx(
          "max-w-xl break-words text-base text-sf-muted",
          align === "center" ? "mx-auto" : null,
        )}
      >
        {description}
      </p>
      {action ? <div className={align === "center" ? "mt-1" : undefined}>{action}</div> : null}
    </div>
  );
}
