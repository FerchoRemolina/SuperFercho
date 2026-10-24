import type { ReactNode } from "react";
import { FerchoFab } from "@/features/assistant/components/fercho-fab";
import { FerchoHost } from "@/features/assistant/components/fercho-host";
import { FerchoShellProvider } from "@/features/assistant/fercho-shell-context";
import { SiteFooter } from "@/shared/ui/site-footer";
import { SiteHeader } from "@/shared/ui/site-header";

export default function StorefrontLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <FerchoShellProvider>
      <SiteHeader />
      {children}
      <FerchoHost />
      <SiteFooter />
      <FerchoFab />
    </FerchoShellProvider>
  );
}
