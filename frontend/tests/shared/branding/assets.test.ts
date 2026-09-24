import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { brandingAssets } from "@/shared/branding/assets";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

describe("branding assets", () => {
  it("reserves the approved SuperFercho asset paths", () => {
    expect(brandingAssets.logo).toBe("/images/branding/superfercho-logo.svg");
    expect(brandingAssets.logoDark).toBe(
      "/images/branding/superfercho-logo-dark.svg",
    );
    expect(brandingAssets.mark).toBe("/images/branding/superfercho-mark.svg");
    expect(brandingAssets.favicon).toBe("/images/branding/favicon.ico");
  });

  it("ships the logo and mark SVG files under public/images/branding", () => {
    expect(
      existsSync(join(frontendRoot, "public/images/branding/superfercho-logo.svg")),
    ).toBe(true);
    expect(
      existsSync(join(frontendRoot, "public/images/branding/superfercho-mark.svg")),
    ).toBe(true);
    expect(
      existsSync(
        join(frontendRoot, "public/images/branding/superfercho-logo-dark.svg"),
      ),
    ).toBe(true);
  });
});
