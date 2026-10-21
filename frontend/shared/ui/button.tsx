import type { ButtonHTMLAttributes } from "react";
import { cx } from "@/shared/utils/cx";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "destructive";

const variants: Record<ButtonVariant, string> = {
  primary:
    "bg-sf-primary text-white shadow-sm hover:bg-sf-primary-hover hover:shadow-md disabled:bg-sf-border disabled:text-sf-muted disabled:shadow-none",
  secondary:
    "border border-sf-border bg-sf-surface text-sf-ink shadow-sm hover:border-sf-primary/40 hover:bg-sf-bg",
  ghost: "text-sf-primary hover:bg-sf-bg",
  destructive: "bg-sf-error text-white shadow-sm hover:opacity-90",
};

export function buttonClassName(
  variant: ButtonVariant = "primary",
  className?: string,
): string {
  return cx(
    "inline-flex min-h-11 items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold",
    "transition-[background-color,border-color,box-shadow,color,opacity,transform] duration-150",
    "active:scale-[0.98] motion-reduce:transition-none motion-reduce:active:transform-none",
    "disabled:cursor-not-allowed disabled:active:scale-100",
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
