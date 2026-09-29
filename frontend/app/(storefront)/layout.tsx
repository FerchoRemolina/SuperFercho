import type { ReactNode } from "react";
import { SiteHeader } from "@/shared/ui/site-header";

export default function StorefrontLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <>
      <SiteHeader />
      {children}
    </>
  );
}
