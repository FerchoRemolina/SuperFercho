import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AppProviders } from "@/app/providers";
import { SiteHeader } from "@/shared/ui/site-header";
import "./globals.css";

export const metadata: Metadata = {
  title: "SuperFercho",
  description: "Supermercado virtual SuperFercho",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="es">
      <body>
        <AppProviders>
          <SiteHeader />
          {children}
        </AppProviders>
      </body>
    </html>
  );
}
