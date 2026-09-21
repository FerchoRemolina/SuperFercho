import type { ReactNode } from "react";
import { cx } from "@/shared/utils/cx";

export function Container({
  children,
  className,
  as: Tag = "div",
  width = "default",
}: {
  children: ReactNode;
  className?: string;
  as?: "div" | "main" | "section" | "header" | "nav";
  width?: "default" | "narrow";
}) {
  return (
    <Tag
      className={cx(
        "mx-auto w-full px-4 md:px-8",
        width === "narrow" ? "max-w-md" : "max-w-6xl",
        className,
      )}
    >
      {children}
    </Tag>
  );
}
