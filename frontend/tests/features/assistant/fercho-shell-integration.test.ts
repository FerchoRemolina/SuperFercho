import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("fercho fab and footer shell integration", () => {
  it("integrates the fab, host and footer in the storefront shell", () => {
    const layout = source("app/(storefront)/layout.tsx");

    expect(layout).toContain("<FerchoShellProvider>");
    expect(layout).toContain("<FerchoHost />");
    expect(layout).toContain("<FerchoFab />");
    expect(layout).toContain("<SiteFooter />");
    expect(layout).not.toContain("AssistantChat");
  });
});
