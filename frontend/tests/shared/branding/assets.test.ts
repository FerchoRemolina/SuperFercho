import { describe, expect, it } from "vitest";
import { brandingAssets } from "@/shared/branding/assets";

describe("branding assets", () => {
  it("reserves the approved SuperFercho asset paths", () => {
    expect(brandingAssets.logo).toBe("/images/branding/superfercho-logo.svg");
    expect(brandingAssets.logoDark).toBe(
      "/images/branding/superfercho-logo-dark.svg",
    );
    expect(brandingAssets.mark).toBe("/images/branding/superfercho-mark.svg");
    expect(brandingAssets.favicon).toBe("/images/branding/favicon.ico");
  });
});
