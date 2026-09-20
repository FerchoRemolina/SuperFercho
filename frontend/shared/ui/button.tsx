import type { ButtonHTMLAttributes } from "react";
import { cx } from "@/shared/utils/cx";

type ButtonVariant = "primary" | "secondary" | "ghost" | "destructive";

const variants: Record<ButtonVariant, string> = {
  primary:
    "bg-sf-accent text-white hover:bg-sf-accent-hover disabled:bg-sf-border",
  secondary:
    "border border-sf-border bg-sf-surface text-sf-ink hover:bg-sf-bg",
  ghost: "text-sf-accent hover:bg-sf-bg",
  destructive: "bg-sf-error text-white hover:opacity-90",
};

export function Button({
  variant = "primary",
  className,
  type = "button",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
}) {
  return (
    <button
      type={type}
      className={cx(
        "inline-flex min-h-10 items-center justify-center rounded-md px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed",
        variants[variant],
        className,
      )}
      {...props}
    />
  );
}
