"use client";

import {
  useId,
  useRef,
  useState,
  type InputHTMLAttributes,
  type PointerEvent,
  type ReactNode,
} from "react";
import { EyeIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

type PasswordFieldProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "type"
> & {
  id: string;
  label: string;
  error?: string;
  valid?: boolean;
  description?: ReactNode;
};

/**
 * Hold-to-reveal password field: visibility only while the eye control is pressed.
 */
export function PasswordField({
  id,
  label,
  error,
  valid,
  className,
  description,
  ...props
}: PasswordFieldProps) {
  const [revealed, setRevealed] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const reactId = useId();
  const revealId = `${id}-reveal-${reactId}`;
  const errorId = error ? `${id}-error` : undefined;
  const descriptionId = description ? `${id}-description` : undefined;
  const describedBy =
    [errorId, descriptionId].filter(Boolean).join(" ") || undefined;

  function hide() {
    setRevealed(false);
  }

  function show(event: PointerEvent<HTMLButtonElement>) {
    event.preventDefault();
    setRevealed(true);
    try {
      event.currentTarget.setPointerCapture(event.pointerId);
    } catch {
      // Some environments reject capture; pointerup/cancel still hide.
    }
  }

  return (
    <div className="grid gap-1">
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      <div className="relative">
        <input
          ref={inputRef}
          id={id}
          type={revealed ? "text" : "password"}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={cx(
            "min-h-11 w-full rounded-lg border border-sf-border bg-sf-surface py-2 pl-3 pr-11 text-base text-sf-ink transition-colors",
            error && "border-sf-error",
            !error && valid && "border-sf-primary/45",
            className,
          )}
          {...props}
        />
        <button
          id={revealId}
          type="button"
          tabIndex={0}
          aria-label="Mantén pulsado para mostrar la contraseña"
          aria-controls={id}
          onPointerDown={show}
          onPointerUp={hide}
          onPointerCancel={hide}
          onPointerLeave={hide}
          onContextMenu={(event) => event.preventDefault()}
          className="absolute inset-y-0 right-0 flex w-11 items-center justify-center rounded-r-lg text-sf-muted transition-colors hover:text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-sf-accent"
        >
          <EyeIcon className="h-5 w-5" aria-hidden="true" />
        </button>
      </div>
      {error ? (
        <p id={errorId} className="text-sm text-sf-error">
          {error}
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
