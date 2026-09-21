import type { InputHTMLAttributes } from "react";
import { cx } from "@/shared/utils/cx";

export function TextField({
  id,
  label,
  error,
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
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
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={errorId}
        className={cx(
          "min-h-11 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink",
          error && "border-sf-error",
          className,
        )}
        {...props}
      />
      {error ? (
        <p id={errorId} className="text-sm text-sf-error">
          {error}
        </p>
      ) : null}
    </div>
  );
}
