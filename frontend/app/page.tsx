import Link from "next/link";
import { HomeProductPreview } from "@/features/catalog/components/home-product-preview";
import { brandingAssets } from "@/shared/branding/assets";
import { Badge } from "@/shared/ui/badge";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";

export default function HomePage() {
  return (
    <main>
      <section className="relative overflow-hidden bg-sf-surface">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-y-0 right-0 hidden w-[48%] bg-gradient-to-br from-sf-yellow-soft via-sf-bg to-sf-surface md:block"
        />
        <Container className="relative grid items-center gap-10 py-10 md:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] md:gap-12 md:py-16 lg:py-20">
          <div className="max-w-xl">
            <h1 className="text-[2rem] font-bold leading-[1.1] tracking-tight text-sf-ink md:text-[2.75rem] lg:text-[3rem]">
              Tu súper, más cerca de ti
            </h1>
            <p className="mt-4 text-base leading-relaxed text-sf-muted md:text-lg">
              Compra tus productos, organiza el mercado y recibe ayuda cuando la
              necesites. Un supermercado digital, práctico y cercano.
            </p>
            <div className="mt-7">
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ver productos
              </Link>
            </div>
          </div>

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
              {/* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */}
              <img
                src={brandingAssets.mark}
                alt=""
                width={220}
                height={220}
                className="h-auto w-full max-w-[11rem] md:max-w-[13rem]"
              />
            </div>
          </div>
        </Container>
      </section>

      <section>
        <Container className="py-12 md:py-16">
          <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="text-[1.5rem] font-bold tracking-tight text-sf-ink md:text-[1.875rem]">
                Productos
              </h2>
              <p className="mt-2 text-sf-muted">
                Lo que puedes llevar a casa hoy.
              </p>
            </div>
            <Link
              href="/catalog"
              className={buttonClassName(
                "ghost",
                "shrink-0 px-0 text-sf-primary hover:bg-transparent hover:underline",
              )}
            >
              Ver catálogo →
            </Link>
          </div>
          <HomeProductPreview />
        </Container>
      </section>

      <section className="pb-16 md:pb-24">
        <Container>
          <Card className="border-sf-yellow/60 bg-sf-yellow-soft">
            <Badge tone="accent">Fercho</Badge>
            <h2 className="mt-4 text-[1.5rem] font-bold tracking-tight text-sf-ink md:text-[1.875rem]">
              Tu asistente en el súper
            </h2>
            <p className="mt-3 max-w-2xl text-base text-sf-muted">
              Fercho te ayuda con productos, carrito, listas y pedidos. Inicia
              sesión como cliente para chatear con él.
            </p>
            <Link
              href="/assistant"
              className={`${buttonClassName("primary")} mt-6 inline-flex`}
            >
              Hablar con Fercho
            </Link>
          </Card>
        </Container>
      </section>
    </main>
  );
}
