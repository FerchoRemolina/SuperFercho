import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

function desktopDropdownItems(header: string): string[] {
  return [...header.matchAll(/href="([^"]+)"\s*\n\s*role="menuitem"/g)].map(
    (match) => match[1],
  );
}

describe("site header account navigation", () => {
  const header = source("shared/ui/site-header.tsx");

  it("trims the desktop Cuenta dropdown to profile, orders and addresses (plus admin)", () => {
    expect(desktopDropdownItems(header)).toEqual([
      "/account/profile",
      "/orders",
      "/addresses",
      "/admin",
    ]);
  });

  it("keeps lists and favorites out of the dropdown but reachable elsewhere", () => {
    // Sin duplicados en el dropdown de escritorio:
    expect(header).not.toMatch(/href="\/lists"\s*\n\s*role="menuitem"/);
    expect(header).not.toMatch(/href="\/favorites"\s*\n\s*role="menuitem"/);
    // Acceso por iconos del encabezado:
    expect(header).toContain('label="Mis listas"');
    expect(header).toContain('<FavoritesHeaderLink variant="header" />');
    expect(header).toContain('<CartHeaderLink variant="header" />');
    // Acceso por menú móvil:
    expect(header).toContain('href="/lists"');
    expect(header).toContain('href="/favorites"');
    expect(header).toContain('href="/cart"');
  });

  it("keeps Mi perfil reachable from desktop dropdown and mobile menu", () => {
    expect(header.match(/href="\/account\/profile"/g)?.length).toBe(2);
    expect(desktopDropdownItems(header)).toContain("/account/profile");
  });

  it("keeps the favorites header icon customer-gated and pointing to /favorites", () => {
    const favoritesLink = source(
      "features/favorites/components/favorites-header-link.tsx",
    );
    expect(favoritesLink).toContain('href="/favorites"');
    expect(favoritesLink).toContain('session?.role !== "CUSTOMER"');
  });
});
