import type { ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

type AlertTone = "info" | "error" | "success" | "warning";

const tones: Record<AlertTone, string> = {
  info: "border-sf-border bg-sf-surface text-sf-ink",
  error: "border-sf-error/40 bg-red-50 text-sf-error",
  success: "border-sf-success/40 bg-green-50 text-sf-success",
  warning: "border-sf-warning/40 bg-amber-50 text-sf-warning",
};

export function Alert({
  tone = "info",
  title,
  children,
}: {
  tone?: AlertTone;
  title: string;
  children?: ReactNode;
}) {
  return (
    <div
      role="alert"
      className={cx("rounded-xl border px-4 py-3", tones[tone])}
    >
      <p className="font-semibold">{title}</p>
      {children ? <div className="mt-1 text-sm">{children}</div> : null}
    </div>
  );
}
