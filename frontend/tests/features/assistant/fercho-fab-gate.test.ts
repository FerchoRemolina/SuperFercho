import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("fercho fab: visibilidad según autenticación", () => {
  const fab = source("features/assistant/components/fercho-fab.tsx");

  it("usa el estado de sesión autoritativo (useSession) como gate", () => {
    expect(fab).toContain("useSession");
    expect(fab).toContain('session?.role !== "CUSTOMER"');
    // Sin dependencias de cookie/indicadores visuales para el gate.
    expect(fab).not.toMatch(/document\.cookie/);
  });

  it("no se muestra en vistas públicas ni se limita a ocultarse visualmente", () => {
    // Return null temprano: sin nodo, sin burbuja residual, sin interceptar clics.
    expect(fab).toMatch(/return null/);
    expect(fab.indexOf("return null")).toBeLessThan(fab.indexOf("<button"));
  });

  it("conserva sus reglas de ubicación existentes", () => {
    expect(fab).toContain('pathname.startsWith("/assistant")');
    expect(fab).toContain('pathname.startsWith("/checkout")');
    expect(fab).not.toContain('href="/assistant"');
    expect(fab).toContain("toggle");
    expect(fab).toContain("brandingAssets.ferchoIcon");
  });
});
