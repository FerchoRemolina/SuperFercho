"use client";

import { useEffect, useId, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "@/shared/session/session-provider";
import { CartHeaderLink } from "@/features/cart/components/cart-header-link";
import { SearchBar } from "@/features/catalog/components/search-bar";
import { BrandLogo } from "@/shared/ui/brand-logo";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import {
  CloseIcon,
  MenuIcon,
  SearchIcon,
  UserIcon,
} from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

const publicLinks = [
  { href: "/", label: "Inicio" },
  { href: "/catalog", label: "Catálogo" },
  { href: "/search", label: "Buscar" },
] as const;

function isActivePath(pathname: string, href: string): boolean {
  if (href === "/") {
    return pathname === "/";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function SiteHeader() {
  const { session, logout } = useSession();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [navPath, setNavPath] = useState(pathname);
  const accountRef = useRef<HTMLDivElement>(null);
  const menuId = useId();
  const isCustomer = session?.role === "CUSTOMER";
  const isAdmin = session?.role === "ADMIN";

  if (navPath !== pathname) {
    setNavPath(pathname);
    setMenuOpen(false);
    setAccountOpen(false);
  }

  useEffect(() => {
    if (!accountOpen) {
      return;
    }
    function onPointerDown(event: PointerEvent) {
      if (!accountRef.current?.contains(event.target as Node)) {
        setAccountOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setAccountOpen(false);
        setMenuOpen(false);
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [accountOpen]);

  return (
    <header className="sticky top-0 z-40 border-b border-sf-border bg-sf-surface">
      <Container className="flex h-16 items-center justify-between gap-4">
        <BrandLogo />

        <nav
          aria-label="Principal"
          className="hidden items-center gap-1 md:flex"
        >
          {publicLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={cx(
                "inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold",
                isActivePath(pathname, link.href)
                  ? "bg-sf-bg text-sf-primary"
                  : "text-sf-ink hover:bg-sf-bg hover:text-sf-primary",
              )}
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="hidden flex-1 justify-center px-4 md:flex">
          <SearchBar compact />
        </div>

        <div className="hidden items-center gap-2 md:flex">
          {session ? (
            <>
              {isCustomer ? <CartHeaderLink /> : null}
              <div className="relative" ref={accountRef}>
                <Button
                  variant="secondary"
                  aria-expanded={accountOpen}
                  aria-haspopup="menu"
                  onClick={() => setAccountOpen((open) => !open)}
                  className="gap-2"
                >
                  <UserIcon />
                  Cuenta
                </Button>
                {accountOpen ? (
                  <div
                    role="menu"
                    aria-label="Cuenta"
                    className="absolute right-0 mt-2 w-48 rounded-xl border border-sf-border bg-sf-surface p-2 shadow-[0_8px_24px_rgba(23,33,27,0.08)]"
                  >
                    {isCustomer ? (
                      <>
                        <Link
                          href="/orders"
                          role="menuitem"
                          className="flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold text-sf-ink hover:bg-sf-bg"
                        >
                          Pedidos
                        </Link>
                        <Link
                          href="/addresses"
                          role="menuitem"
                          className="flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold text-sf-ink hover:bg-sf-bg"
                        >
                          Direcciones
                        </Link>
                      </>
                    ) : null}
                    {isAdmin ? (
                      <Link
                        href="/admin"
                        role="menuitem"
                        className="flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold text-sf-ink hover:bg-sf-bg"
                      >
                        Administración
                      </Link>
                    ) : null}
                    <button
                      type="button"
                      role="menuitem"
                      className="flex min-h-11 w-full items-center rounded-lg px-3 text-left text-sm font-semibold text-sf-ink hover:bg-sf-bg"
                      onClick={logout}
                    >
                      Cerrar sesión
                    </button>
                  </div>
                ) : null}
              </div>
            </>
          ) : (
            <>
              <Link href="/login" className={buttonClassName("ghost")}>
                Entrar
              </Link>
              <Link href="/register" className={buttonClassName("primary")}>
                Crear cuenta
              </Link>
            </>
          )}
        </div>

        <div className="flex items-center gap-1 md:hidden">
          <Link
            href="/search"
            aria-label="Buscar"
            className={buttonClassName("ghost", "px-3")}
          >
            <SearchIcon />
          </Link>
          {isCustomer || !session ? <CartHeaderLink compact /> : null}
          <Button
            variant="ghost"
            className="px-3"
            aria-expanded={menuOpen}
            aria-controls={menuId}
            aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
            onClick={() => setMenuOpen((open) => !open)}
          >
            {menuOpen ? <CloseIcon /> : <MenuIcon />}
          </Button>
        </div>
      </Container>

      {menuOpen ? (
        <div
          id={menuId}
          className="border-t border-sf-border bg-sf-surface md:hidden"
        >
          <Container className="flex flex-col gap-1 py-4">
            <nav aria-label="Móvil" className="flex flex-col gap-1">
              {publicLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className={cx(
                    "flex min-h-12 items-center rounded-lg px-3 text-base font-semibold",
                    isActivePath(pathname, link.href)
                      ? "bg-sf-bg text-sf-primary"
                      : "text-sf-ink hover:bg-sf-bg",
                  )}
                >
                  {link.label}
                </Link>
              ))}
              {isCustomer ? (
                <>
                  <Link
                    href="/cart"
                    className="flex min-h-12 items-center rounded-lg px-3 text-base font-semibold text-sf-ink hover:bg-sf-bg"
                  >
                    Carrito
                  </Link>
                  <Link
                    href="/orders"
                    className="flex min-h-12 items-center rounded-lg px-3 text-base font-semibold text-sf-ink hover:bg-sf-bg"
                  >
                    Pedidos
                  </Link>
                  <Link
                    href="/addresses"
                    className="flex min-h-12 items-center rounded-lg px-3 text-base font-semibold text-sf-ink hover:bg-sf-bg"
                  >
                    Direcciones
                  </Link>
                </>
              ) : null}
              {isAdmin ? (
                <Link
                  href="/admin"
                  className="flex min-h-12 items-center rounded-lg px-3 text-base font-semibold text-sf-ink hover:bg-sf-bg"
                >
                  Administración
                </Link>
              ) : null}
            </nav>
            <div className="mt-3 flex flex-col gap-2 border-t border-sf-border pt-3">
              {session ? (
                <Button variant="secondary" onClick={logout}>
                  Cerrar sesión
                </Button>
              ) : (
                <>
                  <Link href="/login" className={buttonClassName("secondary")}>
                    Entrar
                  </Link>
                  <Link href="/register" className={buttonClassName("primary")}>
                    Crear cuenta
                  </Link>
                </>
              )}
            </div>
          </Container>
        </div>
      ) : null}
    </header>
  );
}
