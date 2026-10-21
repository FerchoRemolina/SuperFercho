import Link from "next/link";
import { HomeProductPreview } from "@/features/catalog/components/home-product-preview";
import { SupportSection } from "@/features/support/components/support-section";
import { brandingAssets } from "@/shared/branding/assets";
import { getServerApiOrigin } from "@/shared/config/env";
import { buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";
import { cx } from "@/shared/utils/cx";
import { HomeValueProps } from "./components/home-value-props";

type HeroContent = {
  title: string;
  description: string;
  /** El contrato REST sigue enviándolo; el Hero ya no lo representa (etapa 1). */
  ctaLabel: string;
  imageUrl: string | null;
  mode: "STANDARD" | "IMMERSIVE";
};

const DEFAULT_HERO: HeroContent = {
  title: "Tu súper, más cerca de ti",
  description:
    "Compra tus productos, organiza el mercado y recibe ayuda cuando la necesites. Un supermercado digital, práctico y cercano.",
  ctaLabel: "Ver productos",
  imageUrl: null,
  mode: "STANDARD",
};

async function loadStorefrontHero(): Promise<HeroContent> {
  try {
    const response = await fetch(
      `${getServerApiOrigin()}/api/v1/storefront-content/hero`,
      { cache: "no-store" },
    );
    if (!response.ok) {
      return DEFAULT_HERO;
    }
    return (await response.json()) as HeroContent;
  } catch {
    return DEFAULT_HERO;
  }
}

export default async function HomePage() {
  const hero = await loadStorefrontHero();

  const heroImage = hero.imageUrl ?? brandingAssets.mark;

  const hasTitle = hero.title.trim() !== "";
  const hasDescription = hero.description.trim() !== "";
  const hasText = hasTitle || hasDescription;

  return (
    <main>
      {hero.mode === "IMMERSIVE" ? (
        <section className="relative overflow-hidden">
          {hero.imageUrl ? (
            /* eslint-disable-next-line @next/next/no-img-element -- imagen configurada desde Media por el Admin */
            <img
              src={heroImage}
              alt=""
              className="absolute inset-0 h-full w-full object-cover object-[62%_center] md:object-center"
            />
          ) : (
            /* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */
            <img
              src={brandingAssets.mark}
              alt=""
              className="absolute inset-0 h-full w-full bg-gradient-to-br from-sf-yellow-soft via-sf-bg to-sf-surface object-cover"
            />
          )}
          <div
            aria-hidden="true"
            className="absolute inset-0 bg-gradient-to-r from-sf-ink/85 via-sf-ink/45 to-transparent"
          />
          <Container width="wide" className="relative flex min-h-[20rem] items-center py-12 md:min-h-[24rem] md:py-20">
            <div className="max-w-xl">
              {hasTitle ? (
                <h1 className="text-[2rem] font-bold leading-[1.1] tracking-tight text-white md:text-[2.75rem] lg:text-[3rem]">
                  {hero.title}
                </h1>
              ) : null}
              {hasDescription ? (
                <p className="mt-4 text-base leading-relaxed text-white/85 md:text-lg">
                  {hero.description}
                </p>
              ) : null}
            </div>
          </Container>
        </section>
      ) : (
        <section className="relative overflow-hidden border-b border-sf-border bg-sf-surface">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-y-0 right-0 hidden w-[48%] bg-gradient-to-br from-sf-yellow-soft via-sf-bg to-sf-surface md:block"
          />
          <Container
            className={cx(
              "relative grid items-center gap-10 py-10 md:gap-12 md:py-16 lg:py-20",
              hasText
                ? "md:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]"
                : "justify-items-center",
            )}
          >
            {hasText ? (
              <div className="max-w-xl">
                {hasTitle ? (
                  <h1 className="text-[2rem] font-bold leading-[1.1] tracking-tight text-sf-ink md:text-[2.75rem] lg:text-[3rem]">
                    {hero.title}
                  </h1>
                ) : null}
                {hasDescription ? (
                  <p className="mt-4 text-base leading-relaxed text-sf-muted md:text-lg">
                    {hero.description}
                  </p>
                ) : null}
              </div>
            ) : null}

            <div className="relative mx-auto flex w-full max-w-md items-center justify-center md:max-w-none md:justify-end">
              <div
                aria-hidden="true"
                className="absolute -right-6 top-4 size-40 rounded-full bg-sf-yellow/50 blur-2xl md:size-56"
              />
              <div
                aria-hidden="true"
                className="absolute -left-4 bottom-2 size-28 rounded-full bg-sf-primary/15 blur-xl md:size-40"
              />
              <div className="relative flex aspect-square w-full max-w-[18rem] items-center justify-center rounded-[2rem] border border-sf-border/80 bg-sf-surface/90 p-8 shadow-[0_12px_40px_rgba(23,33,27,0.08)] md:max-w-[22rem] md:p-10">
                {hero.imageUrl ? (
                  /* eslint-disable-next-line @next/next/no-img-element -- imagen configurada desde Media por el Admin */
                  <img
                    src={hero.imageUrl}
                    alt=""
                    className="h-full w-full rounded-[1.5rem] object-cover"
                  />
                ) : (
                  <>
                    {/* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */}
                    <img
                      src={brandingAssets.mark}
                      alt=""
                      width={220}
                      height={220}
                      className="h-auto w-full max-w-[11rem] md:max-w-[13rem]"
                    />
                  </>
                )}
              </div>
            </div>
          </Container>
        </section>
      )}

      <HomeValueProps />

      <section>
        <Container width="wide" className="py-10 md:py-14">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-x-6 gap-y-3 md:mb-5">
            <div className="max-w-xl">
              <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-primary">
                Catálogo
              </p>
              <h2 className="mt-1 text-[1.5rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[1.875rem]">
                Los más vendidos
              </h2>
              <p className="mt-1.5 text-sm text-sf-muted md:text-base">
                Lo que más están llevando nuestros clientes.
              </p>
            </div>
            <Link
              href="/catalog"
              className={cx(
                buttonClassName("primary"),
                "group/cta shrink-0 gap-2 px-5 shadow-sm",
              )}
            >
              Ver catálogo completo
              <span
                aria-hidden="true"
                className="transition-transform duration-200 group-hover/cta:translate-x-0.5 motion-reduce:transition-none"
              >
                →
              </span>
            </Link>
          </div>
          <HomeProductPreview />
        </Container>
      </section>

      <SupportSection />
    </main>
  );
}
