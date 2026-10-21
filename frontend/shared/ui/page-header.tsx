import type { ReactNode } from "react";
import Link from "next/link";
import { cx } from "@/shared/utils/cx";

/**
 * Encabezado de página estándar del storefront (customer).
 * Unifica jerarquía: navegación de retorno, título, descripción y acciones.
 */
export function PageHeader({
  title,
  description,
  back,
  actions,
  meta,
  className,
  contentClassName,
}: {
  title: string;
  description?: string;
  back?: { href: string; label: string };
  actions?: ReactNode;
  meta?: ReactNode;
  className?: string;
  contentClassName?: string;
}) {
  return (
    <div className={className}>
      {back ? (
        <Link
          href={back.href}
          className={cx(
            "mb-3 inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
            "text-sm font-semibold text-sf-muted transition-colors duration-150",
            "hover:border-sf-primary/40 hover:text-sf-primary",
            "md:mb-4",
          )}
        >
          {back.label}
        </Link>
      ) : null}
      <div
        className={cx(
          "flex flex-wrap items-start justify-between gap-x-6 gap-y-3",
          contentClassName,
        )}
      >
        <div className="min-w-0">
          <h1 className="text-[1.75rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[2.25rem]">
            {title}
          </h1>
          {description ? (
            <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
              {description}
            </p>
          ) : null}
          {meta ? <div className="mt-2.5">{meta}</div> : null}
        </div>
        {actions ? (
          <div className="flex shrink-0 flex-wrap items-center gap-2">
            {actions}
          </div>
        ) : null}
      </div>
    </div>
  );
}
