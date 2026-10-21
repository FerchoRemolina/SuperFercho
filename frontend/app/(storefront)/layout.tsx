import type { ReactNode } from "react";
import { FerchoFab } from "@/features/assistant/components/fercho-fab";
import { SiteFooter } from "@/shared/ui/site-footer";
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
      <SiteFooter />
      <FerchoFab />
    </>
  );
}
