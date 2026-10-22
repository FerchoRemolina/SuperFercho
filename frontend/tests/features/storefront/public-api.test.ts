import { afterEach, describe, expect, it, vi } from "vitest";
import {
  getPublicStorefrontFooterSettings,
  listPublicStorefrontFaqs,
} from "@/features/storefront/api";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("public storefront api", () => {
  it("lists active FAQs from the public endpoint anonymously", async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(
      jsonResponse([
        {
          id: "00000000-0000-0000-0000-00000000fa01",
          question: "¿Cómo puedo realizar un pedido?",
          answer: "Desde el catálogo.",
          displayOrder: 0,
        },
      ]),
    );
    vi.stubGlobal("fetch", fetchMock);

    const faqs = await listPublicStorefrontFaqs();

    expect(faqs).toHaveLength(1);
    expect(faqs[0]?.question).toBe("¿Cómo puedo realizar un pedido?");
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/storefront-content/faqs",
    );
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({
      method: "GET",
    });
    expect(
      (fetchMock.mock.calls[0]?.[1] as RequestInit | undefined)?.headers,
    ).not.toHaveProperty("Authorization");
  });

  it("loads footer settings without inventing defaults for missing QR", async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(
      jsonResponse({
        supportEmail: "SoporteSuperFercho@gmail.com",
        linkedinUrl: "https://www.linkedin.com/in/luis-remolina-0hh0/",
        githubUrl: "https://github.com/FerchoRemolina",
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const settings = await getPublicStorefrontFooterSettings();

    expect(settings).toEqual({
      supportEmail: "SoporteSuperFercho@gmail.com",
      linkedinUrl: "https://www.linkedin.com/in/luis-remolina-0hh0/",
      githubUrl: "https://github.com/FerchoRemolina",
      qrImageUrl: null,
    });
    expect(fetchMock.mock.calls[0]?.[0]).toBe(
      "http://localhost:8080/api/v1/storefront-content/footer-settings",
    );
  });
});

describe("public storefront composition", () => {
  it("does not ship mockup screenshot crops as storefront decorations", async () => {
    const { existsSync } = await import("node:fs");
    const { resolve } = await import("node:path");
    expect(
      existsSync(resolve(process.cwd(), "public/images/storefront")),
    ).toBe(false);
  });

  it("wires FAQ on home and verifies FAQ/footer components independently", async () => {
    const { existsSync, readFileSync } = await import("node:fs");
    const { resolve } = await import("node:path");

    const support = readFileSync(
      resolve(process.cwd(), "features/support/components/support-section.tsx"),
      "utf8",
    );
    const footer = readFileSync(
      resolve(process.cwd(), "shared/ui/site-footer.tsx"),
      "utf8",
    );
    const home = readFileSync(
      resolve(process.cwd(), "app/(storefront)/page.tsx"),
      "utf8",
    );
    const layout = readFileSync(
      resolve(process.cwd(), "app/(storefront)/layout.tsx"),
      "utf8",
    );
    const assets = readFileSync(
      resolve(process.cwd(), "shared/branding/assets.ts"),
      "utf8",
    );

    expect(home).toContain("SupportSection");
    expect(layout).not.toContain("SupportSection");
    expect(layout).not.toContain("SupportSection");

    expect(support).toContain("usePublicStorefrontFaqsQuery");
    expect(support).toContain("SupportEmailActions");
    expect(support).toContain("FerchoSupportMascot");
    expect(support).toContain("FaqLeafDecorations");
    expect(support).not.toContain("LeafSpray");
    expect(support).not.toContain("ProduceCorner");
    expect(support).not.toContain("storefrontDecorAssets");
    expect(support).not.toContain("fercho-cart");
    expect(support).not.toContain("brandingAssets.ferchoIcon");

    expect(footer).toContain("usePublicStorefrontFooterSettingsQuery");
    expect(footer).toContain("FooterWave");
    expect(footer).toContain("brandingAssets.tomates");
    expect(footer).toContain("brandingAssets.bolsaDeCompras");
    expect(footer).not.toContain("GroceryBagDeco");
    expect(footer).not.toContain("LeafSpray");
    expect(footer).not.toContain("ProduceCorner");
    expect(footer).toContain("SupportEmailActions");
    expect(footer).not.toContain("storefrontDecorAssets");
    expect(footer).not.toContain("images/storefront");

    expect(assets).toContain("ferchoIcon");
    expect(assets).toContain("ferchoMascot");
    expect(assets).toContain("fercho-comprando.png");
    expect(assets).toContain("tomates.png");
    expect(assets).toContain("bolsa-de-compras.png");
    expect(assets).toContain("hojas izquierdas.png");
    expect(assets).toContain("hojas derechas.png");
    expect(assets).not.toContain("storefrontDecorAssets");

    const mascot = readFileSync(
      resolve(process.cwd(), "features/support/components/fercho-support-mascot.tsx"),
      "utf8",
    );
    expect(mascot).toContain("ferchoMascot");
    expect(mascot).not.toContain("ferchoIcon");

    expect(
      existsSync(
        resolve(process.cwd(), "public/images/branding/fercho-comprando.png"),
      ),
    ).toBe(true);
    expect(
      existsSync(resolve(process.cwd(), "public/images/branding/tomates.png")),
    ).toBe(true);
    expect(
      existsSync(
        resolve(process.cwd(), "public/images/branding/bolsa-de-compras.png"),
      ),
    ).toBe(true);
    expect(
      existsSync(
        resolve(process.cwd(), "public/images/branding/hojas izquierdas.png"),
      ),
    ).toBe(true);
    expect(
      existsSync(
        resolve(process.cwd(), "public/images/branding/hojas derechas.png"),
      ),
    ).toBe(true);
  });
});
