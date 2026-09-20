"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import type { Role } from "@/shared/session/session";
import { useSession } from "@/shared/session/session-provider";

export function RequireRole({
  role,
  children,
}: {
  role: Role;
  children: ReactNode;
}) {
  const { session } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (!session) {
      router.replace("/login");
      return;
    }
    if (session.role !== role) {
      router.replace("/");
    }
  }, [role, router, session]);

  if (!session || session.role !== role) {
    return (
      <p className="px-4 py-8 text-sf-muted" role="status">
        Comprobando acceso…
      </p>
    );
  }

  return children;
}
