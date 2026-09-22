"use client";

import type { ReactNode } from "react";
import { AdminSectionNav } from "@/features/admin/components/admin-section-nav";

export function AdminShell({ children }: { children: ReactNode }) {
  return (
    <>
      <AdminSectionNav />
      {children}
    </>
  );
}
