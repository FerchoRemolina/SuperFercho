import type { SelectHTMLAttributes } from "react";
import { cx } from "@/shared/utils/cx";

export function SelectField({
  id,
  label,
  error,
  className,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & {
  id: string;
  label: string;
  error?: string;
}) {
  const errorId = error ? `${id}-error` : undefined;

  return (
    <div className="grid gap-1">
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={errorId}
        className={cx(
          "min-h-11 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink",
          error && "border-sf-error",
          className,
        )}
        {...props}
      >
        {children}
      </select>
      {error ? (
        <p id={errorId} className="text-sm text-sf-error">
          {error}
        </p>
      ) : null}
    </div>
  );
}
