import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("admin fercho supervision page", () => {
  const page = source("features/admin/components/admin-fercho-page.tsx");
  const api = source("features/admin/api.ts");
  const nav = source("features/admin/presentation.ts");

  it("is wired into the Fercho admin navigation", () => {
    expect(nav).toContain('href: "/admin/fercho"');
    expect(nav).toContain("Supervisión de uso");
  });

  it("consumes the real admin endpoints", () => {
    expect(api).toContain("/admin/fercho/summary");
    expect(api).toContain("/admin/fercho/alerts");
    expect(api).toContain("/admin/fercho/restrictions");
    expect(page).toContain("getAdminFerchoSummary");
    expect(page).toContain("getAdminFerchoAlerts");
    expect(page).toContain("resolveAdminFerchoAlert");
    expect(page).toContain("liftAdminFerchoRestriction");
  });

  it("labels the cost as an estimate and shows empty state when unknown", () => {
    expect(page).toContain("Costo estimado");
    expect(page).toContain("No disponible");
    expect(page).toContain("estimatedCost ??");
  });

  it("exposes decision and lift actions guarded by the working flag", () => {
    expect(page).toContain('"DISMISS"');
    expect(page).toContain('"WARN"');
    expect(page).toContain('"SUSPEND"');
    expect(page).toContain('lift(restriction.id)');
    expect(page).toContain("working !== null");
  });

  it("renders loading, error and empty states", () => {
    expect(page).toContain("isPending");
    expect(page).toContain("isError");
    expect(page).toContain("Sin alertas.");
    expect(page).toContain("Sin restricciones activas.");
  });
});
