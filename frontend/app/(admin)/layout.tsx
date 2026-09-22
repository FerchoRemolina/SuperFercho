import type { ReactNode } from "react";
import { AdminShell } from "@/features/admin/components/admin-shell";
import { RequireRole } from "@/shared/auth/require-role";

export default function AdminGroupLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <RequireRole role="ADMIN">
      <AdminShell>{children}</AdminShell>
    </RequireRole>
  );
}
