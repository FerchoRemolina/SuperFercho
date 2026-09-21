import { cx } from "@/shared/utils/cx";

export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={cx("animate-pulse rounded-xl bg-sf-border/70", className)}
    />
  );
}
