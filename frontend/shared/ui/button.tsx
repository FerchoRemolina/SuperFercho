import type { ButtonHTMLAttributes } from "react";
import { cx } from "@/shared/utils/cx";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "destructive";

const variants: Record<ButtonVariant, string> = {
  primary:
    "bg-sf-primary text-white hover:bg-sf-primary-hover disabled:bg-sf-border disabled:text-sf-muted",
  secondary:
    "border border-sf-border bg-sf-surface text-sf-ink hover:bg-sf-bg",
  ghost: "text-sf-primary hover:bg-sf-bg",
  destructive: "bg-sf-error text-white hover:opacity-90",
};

export function buttonClassName(
  variant: ButtonVariant = "primary",
  className?: string,
): string {
  return cx(
    "inline-flex min-h-11 items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed",
    variants[variant],
    className,
  );
}

export function Button({
  variant = "primary",
  className,
  type = "button",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
}) {
  return (
    <button type={type} className={buttonClassName(variant, className)} {...props} />
  );
}
