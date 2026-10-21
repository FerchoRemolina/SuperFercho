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
  width?: "default" | "narrow" | "wide";
}) {
  return (
    <Tag
      className={cx(
        "mx-auto w-full px-4 md:px-8",
        width === "narrow" && "max-w-md",
        width === "wide" && "max-w-[1440px]",
        width === "default" && "max-w-6xl",
        className,
      )}
    >
      {children}
    </Tag>
  );
}
