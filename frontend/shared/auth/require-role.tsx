"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { safeNextPath } from "@/shared/auth/safe-next-path";
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
  const pathname = usePathname();

  useEffect(() => {
    if (!session) {
      const next = safeNextPath(pathname);
      router.replace(next ? `/login?next=${encodeURIComponent(next)}` : "/login");
      return;
    }
    if (session.role !== role) {
      router.replace("/");
    }
  }, [pathname, role, router, session]);

  if (!session || session.role !== role) {
    return (
      <p className="px-4 py-8 text-base text-sf-muted" role="status">
        Comprobando acceso…
      </p>
    );
  }

  return children;
}
