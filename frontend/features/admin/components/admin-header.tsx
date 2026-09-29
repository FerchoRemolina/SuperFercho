"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";
import { StorefrontPreviewEnterButton } from "@/features/admin/components/storefront-preview-enter-button";
import { adminBreadcrumbsForPath } from "@/features/admin/presentation";
import { useSession } from "@/shared/session/session-provider";
import {
  accountAvatarInitial,
  accountMenuDisplayName,
} from "@/shared/ui/account-menu-presentation";
import { ChevronIcon, LogOutIcon, MenuIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

export function AdminHeader({
  onOpenMobileNav,
}: {
  onOpenMobileNav?: () => void;
}) {
  const pathname = usePathname();
  const { session, logout } = useSession();
  const crumbs = adminBreadcrumbsForPath(pathname);
  const displayName =
    accountMenuDisplayName({
      firstName: session?.firstName,
      isPreview: false,
    }) ?? "Admin";
  const initial = accountAvatarInitial(displayName);
  const menuId = useId();
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }
    function onPointerDown(event: MouseEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-30 border-b border-sf-border bg-sf-surface/95 backdrop-blur-sm">
      <div className="flex h-16 items-center gap-4 px-4 md:px-6 lg:px-8">
        <button
          type="button"
          className={cx(
            "inline-flex h-10 w-10 items-center justify-center rounded-xl border border-sf-border text-sf-ink lg:hidden",
            "hover:bg-sf-bg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
          )}
          aria-label="Abrir menú de administración"
          onClick={onOpenMobileNav}
        >
          <MenuIcon className="h-5 w-5" />
        </button>

        <nav
          aria-label="Ruta"
          className="min-w-0 flex-1 overflow-hidden text-sm text-sf-muted"
        >
          <ol className="flex flex-wrap items-center gap-1.5">
            {crumbs.map((crumb, index) => {
              const isLast = index === crumbs.length - 1;
              return (
                <li
                  key={`${crumb.label}-${index}`}
                  className="flex items-center gap-1.5"
                >
                  {index > 0 ? <span aria-hidden="true">›</span> : null}
                  {crumb.href && !isLast ? (
                    <Link
                      href={crumb.href}
                      className="truncate font-medium hover:text-sf-primary"
                    >
                      {crumb.label}
                    </Link>
                  ) : (
                    <span
                      className={cx(
                        "truncate",
                        isLast ? "font-semibold text-sf-ink" : "font-medium",
                      )}
                    >
                      {crumb.label}
                    </span>
                  )}
                </li>
              );
            })}
          </ol>
        </nav>

        <div className="flex shrink-0 items-center gap-3 border-l border-sf-border pl-3 md:gap-4 md:pl-4">
          <StorefrontPreviewEnterButton />
          <div className="relative" ref={menuRef}>
            <button
              type="button"
              className={cx(
                "flex items-center gap-2.5 rounded-xl px-2 py-1.5 transition-colors hover:bg-sf-bg",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
              )}
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              aria-controls={menuId}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <span
                className="flex h-9 w-9 items-center justify-center rounded-full bg-sf-bg text-sm font-bold text-sf-ink ring-1 ring-sf-border"
                aria-hidden="true"
              >
                {initial}
              </span>
              <span className="hidden leading-tight text-left sm:block">
                <span className="block text-sm font-semibold text-sf-ink">
                  {displayName}
                </span>
                <span className="block text-xs text-sf-muted">
                  Administrador
                </span>
              </span>
              <ChevronIcon
                className={cx(
                  "hidden h-3.5 w-3.5 text-sf-muted sm:block transition-transform",
                  menuOpen && "rotate-90",
                )}
              />
            </button>
            {menuOpen ? (
              <div
                id={menuId}
                role="menu"
                className="absolute right-0 mt-2 w-48 overflow-hidden rounded-xl border border-sf-border bg-sf-surface py-1 shadow-[0_8px_24px_rgba(23,33,27,0.12)]"
              >
                <button
                  type="button"
                  role="menuitem"
                  className="flex w-full min-h-11 items-center gap-2.5 px-3 text-left text-sm font-semibold text-sf-ink hover:bg-sf-bg"
                  onClick={() => {
                    setMenuOpen(false);
                    logout();
                  }}
                >
                  <LogOutIcon className="h-4 w-4 text-sf-muted" />
                  Cerrar sesión
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
