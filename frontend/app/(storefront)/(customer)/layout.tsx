import type { ReactNode } from "react";
import { RequireRole } from "@/shared/auth/require-role";

export default function CustomerLayout({
  children,
}: {
  children: ReactNode;
}) {
  return <RequireRole role="CUSTOMER">{children}</RequireRole>;
}
