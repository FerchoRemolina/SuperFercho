import type { ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

type BadgeTone = "neutral" | "primary" | "accent" | "danger";

const tones: Record<BadgeTone, string> = {
  neutral: "bg-sf-bg text-sf-muted",
  primary: "bg-sf-primary/10 text-sf-primary",
  accent: "bg-sf-yellow-soft text-sf-ink",
  danger: "bg-sf-error/10 text-sf-error",
};

export function Badge({
  children,
  tone = "neutral",
  className,
}: {
  children: ReactNode;
  tone?: BadgeTone;
  className?: string;
}) {
  return (
    <span
      className={cx(
        "inline-flex items-center rounded-lg px-2.5 py-1 text-sm font-semibold",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
