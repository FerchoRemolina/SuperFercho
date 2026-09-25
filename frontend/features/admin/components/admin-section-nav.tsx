"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { StorefrontPreviewEnterButton } from "@/features/admin/components/storefront-preview-enter-button";
import { ADMIN_NAV_LINKS, isAdminNavActive } from "@/features/admin/presentation";
import { cx } from "@/shared/utils/cx";

export function AdminSectionNav() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Administración"
      className="border-b border-sf-border bg-sf-surface"
    >
      <div className="mx-auto flex max-w-6xl items-center gap-2 overflow-x-auto px-4 py-2 md:px-8">
        <div className="flex min-w-0 flex-1 gap-1">
          {ADMIN_NAV_LINKS.map((link) => {
            const active = isAdminNavActive(link.href, pathname, link.match);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={cx(
                  "min-h-11 shrink-0 rounded-lg px-3 py-2 text-sm font-semibold",
                  active
                    ? "bg-sf-primary/10 text-sf-primary"
                    : "text-sf-muted hover:bg-sf-bg hover:text-sf-ink",
                )}
                aria-current={active ? "page" : undefined}
              >
                {link.label}
              </Link>
            );
          })}
        </div>
        <StorefrontPreviewEnterButton className="shrink-0" />
      </div>
    </nav>
  );
}
