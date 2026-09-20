import type { ReactNode } from "react";
import { RequireRole } from "@/shared/auth/require-role";

export default function AdminGroupLayout({
  children,
}: {
  children: ReactNode;
}) {
  return <RequireRole role="ADMIN">{children}</RequireRole>;
}
