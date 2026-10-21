"use client";

import { useEffect, useId, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "@/shared/session/session-provider";
import { CustomerSessionCountdown } from "@/shared/session/customer-session-countdown";
import { StorefrontPreviewBanner } from "@/shared/session/storefront-preview-banner";
import { CartHeaderLink } from "@/features/cart/components/cart-header-link";
import { FavoritesHeaderLink } from "@/features/favorites/components/favorites-header-link";
import { SearchBar } from "@/features/catalog/components/search-bar";
import { BrandLogo } from "@/shared/ui/brand-logo";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import {
  ClipboardListIcon,
  CloseIcon,
  GridIcon,
  HeartIcon,
  LogOutIcon,
  MapPinIcon,
  MenuIcon,
  PackageIcon,
  UserIcon,
} from "@/shared/ui/icons";
import { HeaderIconAction } from "@/shared/ui/header-icon-action";
import {
  accountAvatarInitial,
  accountMenuDisplayName,
} from "@/shared/ui/account-menu-presentation";
import { cx } from "@/shared/utils/cx";

const publicLinks = [
  { href: "/catalog", label: "Catálogo" },
] as const;

function isActivePath(pathname: string, href: string): boolean {
  if (href === "/") {
    return pathname === "/";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function SiteHeader() {
  const {
    session,
    logout,
    isPreview,
    previewMeta,
    returnToAdmin,
    exitStorefrontPreviewMode,
  } = useSession();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [accountMenuMounted, setAccountMenuMounted] = useState(false);
  const [accountMenuShown, setAccountMenuShown] = useState(false);
  const [navPath, setNavPath] = useState(pathname);
  const accountRef = useRef<HTMLDivElement>(null);
  const menuId = useId();
  const isCustomer = session?.role === "CUSTOMER";
  const isAdmin = session?.role === "ADMIN";
  const accountName = accountMenuDisplayName({
    firstName: session?.firstName,
    isPreview,
  });
  const accountInitial = accountAvatarInitial(accountName ?? "");

  const accountMenuItemClass = cx(
    "flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-semibold text-sf-ink",
    "transition-colors duration-150 hover:bg-sf-bg",
    "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
  );

  if (navPath !== pathname) {
    setNavPath(pathname);
    setMenuOpen(false);
    setAccountOpen(false);
  }

  useEffect(() => {
    if (accountOpen) {
      setAccountMenuMounted(true);
      const frame = window.requestAnimationFrame(() => {
        setAccountMenuShown(true);
      });
      return () => window.cancelAnimationFrame(frame);
    }
    setAccountMenuShown(false);
    const timeout = window.setTimeout(() => {
      setAccountMenuMounted(false);
    }, 180);
    return () => window.clearTimeout(timeout);
  }, [accountOpen]);

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
    <header className="sticky top-0 z-40 bg-sf-surface/95 backdrop-blur-sm">
      {isPreview && previewMeta ? (
        <StorefrontPreviewBanner
          previewExpiresAt={previewMeta.previewExpiresAt}
          onReturnToAdmin={returnToAdmin}
          onExitPreview={() => {
            void exitStorefrontPreviewMode();
          }}
        />
      ) : null}
      <div className="border-b border-sf-border shadow-[0_1px_2px_rgba(23,33,27,0.04)]">
      <Container width="wide" className="flex h-16 items-center justify-between gap-3 md:gap-4">
        <div className="shrink-0">
          <BrandLogo />
        </div>

        {/* Búsqueda global: zona flexible prioritaria del encabezado. */}
        <div className="hidden min-w-0 flex-1 justify-center lg:flex">
          <SearchBar compact />
        </div>

        {/* Zona derecha: sesión, Catálogo y acciones por icono con etiqueta.
            Sin separadores verticales: la jerarquía se resuelve con espaciado. */}
        <div className="hidden items-center gap-2 whitespace-nowrap md:flex md:shrink-0">
          {session ? (
            <>
              <div className="hidden lg:block">
                <CustomerSessionCountdown />
              </div>
              <Link
                href="/catalog"
                aria-current={isActivePath(pathname, "/catalog") ? "page" : undefined}
                className={cx(
                  "inline-flex min-h-11 items-center gap-2 rounded-lg px-3 text-[15px] font-semibold leading-tight transition-colors",
                  isActivePath(pathname, "/catalog")
                    ? "bg-sf-primary/10 text-sf-primary"
                    : "text-sf-ink hover:bg-sf-bg hover:text-sf-primary",
                )}
              >
                <GridIcon className="h-[18px] w-[18px]" strokeWidth={2} />
                Catálogo
              </Link>
            </>
          ) : null}
          {session ? (
            <>
              {isCustomer ? (
                <>
                  <HeaderIconAction
                    label="Mis listas"
                    href="/lists"
                    icon={                    <ClipboardListIcon
                      width={26}
                      height={26}
                      strokeWidth={2}
                      className="shrink-0"
                    />}
                  />
                  <FavoritesHeaderLink variant="header" />
                  <CartHeaderLink variant="header" />
                </>
              ) : null}
              <div className="relative" ref={accountRef}>
                <HeaderIconAction
                  label="Cuenta"
                  aria-expanded={accountOpen}
                  icon={                  <UserIcon
                    width={26}
                    height={26}
                    strokeWidth={2}
                    className="shrink-0"
                  />}
                  onClick={() => setAccountOpen((open) => !open)}
                />
                {accountMenuMounted ? (
                  <div
                    role="menu"
                    aria-label="Cuenta"
                    className={cx(
                      "absolute right-0 z-50 mt-2 w-64 max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl",
                      "origin-top border border-sf-border bg-sf-surface p-2",
                      "shadow-[0_10px_28px_rgba(23,33,27,0.1)]",
                      "transition-[opacity,transform] duration-[170ms] ease-out",
                      "motion-reduce:transition-none",
                      accountMenuShown
                        ? "translate-y-0 scale-y-100 opacity-100"
                        : "pointer-events-none -translate-y-1 scale-y-95 opacity-0",
                    )}
                  >
                    {isCustomer ? (
                      <div className="flex items-center gap-3 px-3 py-2.5">
                        <span
                          className={cx(
                            "flex h-10 w-10 shrink-0 items-center justify-center rounded-full",
                            "bg-sf-primary/10 text-sm font-bold text-sf-primary",
                          )}
                          aria-hidden="true"
                        >
                          {accountInitial}
                        </span>
                        <div className="min-w-0">
                          {accountName ? (
                            <p className="truncate text-sm font-semibold text-sf-ink">
                              {accountName}
                            </p>
                          ) : null}
                          <p className="text-xs text-sf-muted">Mi cuenta</p>
                        </div>
                      </div>
                    ) : null}

                    {isCustomer ? (
                      <>
                        <div
                          className="my-1.5 border-t border-sf-border/90"
                          role="separator"
                        />
                        <div className="grid gap-0.5 py-0.5">
                          <Link
                            href="/lists"
                            role="menuitem"
                            className={accountMenuItemClass}
                          >
                            <ClipboardListIcon className="h-[18px] w-[18px] shrink-0 text-sf-muted" />
                            Mis listas
                          </Link>
                          <Link
                            href="/favorites"
                            role="menuitem"
                            className={accountMenuItemClass}
                          >
                            <HeartIcon className="h-[18px] w-[18px] shrink-0 text-sf-muted" />
                            Favoritos
                          </Link>
                          <Link
                            href="/orders"
                            role="menuitem"
                            className={accountMenuItemClass}
                          >
                            <PackageIcon className="h-[18px] w-[18px] shrink-0 text-sf-muted" />
                            Pedidos
                          </Link>
                          <Link
                            href="/addresses"
                            role="menuitem"
                            className={accountMenuItemClass}
                          >
                            <MapPinIcon className="h-[18px] w-[18px] shrink-0 text-sf-muted" />
                            Direcciones
                          </Link>
                        </div>
                      </>
                    ) : null}

                    {isAdmin ? (
                      <>
                        {isCustomer ? (
                          <div
                            className="my-1.5 border-t border-sf-border/90"
                            role="separator"
                          />
                        ) : null}
                        <Link
                          href="/admin"
                          role="menuitem"
                          className={accountMenuItemClass}
                        >
                          <UserIcon className="h-[18px] w-[18px] shrink-0 text-sf-muted" />
                          Administración
                        </Link>
                      </>
                    ) : null}

                    {isPreview ? (
                      <button
                        type="button"
                        role="menuitem"
                        className={cx(accountMenuItemClass, "w-full text-left")}
                        onClick={() => {
                          setAccountOpen(false);
                          returnToAdmin();
                        }}
                      >
                        <UserIcon className="h-[18px] w-[18px] shrink-0 text-sf-muted" />
                        Volver a administración
                      </button>
                    ) : null}

                    <div
                      className="my-1.5 border-t border-sf-border/90"
                      role="separator"
                    />
                    <button
                      type="button"
                      role="menuitem"
                      className={cx(
                        "flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-left text-sm font-semibold",
                        "text-sf-error transition-colors duration-150",
                        "hover:bg-red-50",
                        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-error",
                      )}
                      onClick={() => {
                        setAccountOpen(false);
                        if (isPreview) {
                          void exitStorefrontPreviewMode();
                          return;
                        }
                        logout();
                      }}
                    >
                      <LogOutIcon className="h-[18px] w-[18px] shrink-0" />
                      {isPreview ? "Salir de preview" : "Cerrar sesión"}
                    </button>
                  </div>
                ) : null}
              </div>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className={buttonClassName("secondary", "min-h-10 px-5")}
              >
                Entrar
              </Link>
              <Link
                href="/register"
                className={buttonClassName("primary", "min-h-10 px-5")}
              >
                Crear cuenta
              </Link>
            </>
          )}
        </div>

        <div className="flex items-center gap-1 md:hidden">
          {isCustomer ? (
            <Link
              href="/favorites"
              aria-label="Favoritos"
              className={buttonClassName("ghost", "px-3")}
            >
              <HeartIcon />
            </Link>
          ) : null}
          {isCustomer || !session ? <CartHeaderLink compact /> : null}
        </div>
        <Button
          variant="ghost"
          className="px-3 md:hidden"
          aria-expanded={menuOpen}
          aria-controls={menuId}
          aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? <CloseIcon /> : <MenuIcon />}
        </Button>
      </Container>

      {/* Tablet (md–lg): la búsqueda ocupa una fila propia a ancho completo
          para que el placeholder nunca se comprima. */}
      <div className="flex justify-center border-b border-sf-border px-4 pb-3 md:flex lg:hidden">
        <SearchBar compact id="header-search-tablet" />
      </div>

      {menuOpen ? (
        <div
          id={menuId}
          className="border-t border-sf-border bg-sf-surface xl:hidden"
        >
          <Container width="wide" className="flex flex-col gap-1 py-4">
            <nav aria-label="Móvil" className="flex flex-col gap-1">
              {publicLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className={cx(
                    "flex min-h-12 items-center rounded-lg px-3 text-base font-semibold",
                    isActivePath(pathname, link.href)
                      ? "bg-sf-primary/10 text-sf-primary"
                      : "text-sf-ink hover:bg-sf-bg",
                  )}
                >
                  {link.label}
                </Link>
              ))}
              {isCustomer ? (
                <>
                  <Link
                    href="/lists"
                    className={cx(
                      "flex min-h-12 items-center rounded-lg px-3 text-base font-semibold",
                      isActivePath(pathname, "/lists")
                        ? "bg-sf-primary/10 text-sf-primary"
                        : "text-sf-ink hover:bg-sf-bg",
                    )}
                  >
                    Mis listas
                  </Link>
                  <Link
                    href="/favorites"
                    className={cx(
                      "flex min-h-12 items-center rounded-lg px-3 text-base font-semibold",
                      isActivePath(pathname, "/favorites")
                        ? "bg-sf-primary/10 text-sf-primary"
                        : "text-sf-ink hover:bg-sf-bg",
                    )}
                  >
                    Favoritos
                  </Link>
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
              {isPreview ? (
                <>
                  <p className="px-3 text-sm font-semibold text-sf-ink">
                    Cliente de prueba
                  </p>
                  <p className="-mt-1 px-3 text-xs text-sf-muted">
                    Customer temporal · Modo de prueba
                  </p>
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setMenuOpen(false);
                      returnToAdmin();
                    }}
                  >
                    Volver a administración
                  </Button>
                  <Button
                    variant="destructive"
                    onClick={() => {
                      setMenuOpen(false);
                      void exitStorefrontPreviewMode();
                    }}
                  >
                    Salir de preview
                  </Button>
                </>
              ) : session ? (
                <Button variant="secondary" onClick={logout}>
                  Cerrar sesión
                </Button>
          ) : (
            <div className="flex items-center gap-3">
              <Link
                href="/login"
                className={buttonClassName("secondary", "min-h-10 px-5")}
              >
                Entrar
              </Link>
              <Link
                href="/register"
                className={buttonClassName("primary", "min-h-10 px-5")}
              >
                Crear cuenta
              </Link>
            </div>
          )}
            </div>
          </Container>
        </div>
      ) : null}
      </div>
    </header>
  );
}
