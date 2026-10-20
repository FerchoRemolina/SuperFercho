import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("fercho assistant presence", () => {
  it("is not a text navigation option in the header", () => {
    const header = source("shared/ui/site-header.tsx");
    expect(header).not.toContain('href="/assistant"');
  });

  it("lives as a persistent floating assistant in the storefront", () => {
    const fab = source("features/assistant/components/fercho-fab.tsx");

    expect(fab).toContain("brandingAssets.ferchoIcon");
    expect(fab).toContain('href="/assistant"');
    expect(fab).toContain("fixed bottom-");
    expect(fab).toContain("motion-reduce");
    expect(fab).toContain("aria-label");
  });

  it("hides the fab where it would interfere with content", () => {
    const fab = source("features/assistant/components/fercho-fab.tsx");
    expect(fab).toContain('pathname.startsWith("/assistant")');
    expect(fab).toContain('pathname.startsWith("/checkout")');
  });

  it("no longer renders the large Fercho block at the end of Home", () => {
    const page = source("app/(storefront)/page.tsx");
    expect(page).not.toContain("Tu asistente en el súper");
    expect(page).not.toContain("Hablar con Fercho");
  });
});
