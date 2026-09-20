"use client";

import Link from "next/link";
import { useSession } from "@/shared/session/session-provider";
import { Button } from "@/shared/ui/button";
import { cx } from "@/shared/utils/cx";

const publicLinks = [
  { href: "/", label: "Inicio" },
  { href: "/catalog", label: "Catálogo" },
  { href: "/search", label: "Buscar" },
];

const customerLinks = [
  { href: "/cart", label: "Carrito" },
  { href: "/orders", label: "Pedidos" },
  { href: "/lists", label: "Listas" },
  { href: "/addresses", label: "Direcciones" },
  { href: "/assistant", label: "Asistente" },
];

const adminLinks = [{ href: "/admin", label: "Admin" }];

export function SiteHeader() {
  const { session, logout } = useSession();
  const roleLinks =
    session?.role === "CUSTOMER"
      ? customerLinks
      : session?.role === "ADMIN"
        ? adminLinks
        : [];

  return (
    <header className="border-b border-sf-border bg-sf-surface">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <Link href="/" className="text-lg font-semibold text-sf-accent">
          SuperFercho
        </Link>
        <nav aria-label="Principal" className="flex flex-wrap gap-3 text-sm">
          {[...publicLinks, ...roleLinks].map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-sf-ink hover:text-sf-accent"
            >
              {link.label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          {session ? (
            <Button variant="ghost" onClick={logout}>
              Cerrar sesión
            </Button>
          ) : (
            <>
              <Link href="/login" className="text-sm font-semibold text-sf-accent">
                Entrar
              </Link>
              <Link
                href="/register"
                className={cx(
                  "inline-flex min-h-10 items-center justify-center rounded-md border border-sf-border bg-sf-surface px-4 py-2 text-sm font-semibold text-sf-ink hover:bg-sf-bg",
                )}
              >
                Crear cuenta
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
