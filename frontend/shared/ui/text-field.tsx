import type { InputHTMLAttributes, ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

export function TextField({
  id,
  label,
  error,
  hint,
  valid,
  className,
  description,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  id: string;
  label: string;
  error?: string;
  hint?: string;
  valid?: boolean;
  description?: ReactNode;
}) {
  const errorId = error ? `${id}-error` : undefined;
  const hintId = hint && !error ? `${id}-hint` : undefined;
  const descriptionId = description ? `${id}-description` : undefined;
  const describedBy =
    [errorId, hintId, descriptionId].filter(Boolean).join(" ") || undefined;

  return (
    <div className="grid gap-1">
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cx(
          "min-h-11 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink transition-colors",
          error && "border-sf-error",
          !error && valid && "border-sf-primary/45",
          className,
        )}
        {...props}
      />
      {error ? (
        <p id={errorId} className="text-sm text-sf-error">
          {error}
        </p>
      ) : hint ? (
        <p id={hintId} className="text-sm text-sf-muted">
          {hint}
        </p>
      ) : null}
      {description ? (
        <div id={descriptionId} className="mt-1">
          {description}
        </div>
      ) : null}
    </div>
  );
}
