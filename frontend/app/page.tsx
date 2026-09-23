import Link from "next/link";
import { HomeProductPreview } from "@/features/catalog/components/home-product-preview";
import { Badge } from "@/shared/ui/badge";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { CartIcon, ChevronIcon, SearchIcon } from "@/shared/ui/icons";

const quickAccess = [
  {
    href: "/catalog",
    title: "Catálogo",
    description: "Explora los productos del súper.",
    icon: ChevronIcon,
  },
  {
    href: "/search",
    title: "Buscar",
    description: "Encuentra lo que necesitas más rápido.",
    icon: SearchIcon,
  },
  {
    href: "/cart",
    title: "Carrito",
    description: "Revisa tu mercado cuando inicies sesión.",
    icon: CartIcon,
  },
] as const;

export default function HomePage() {
  return (
    <main>
      <section className="bg-sf-surface">
        <Container className="grid gap-8 py-12 md:py-20">
          <div className="max-w-2xl">
            <p className="text-sm font-semibold text-sf-primary">
              SuperFercho
            </p>
            <h1 className="mt-3 text-[2rem] font-bold leading-tight tracking-tight text-sf-ink md:text-[2.75rem]">
              Tu súper, más cerca de ti
            </h1>
            <p className="mt-4 max-w-xl text-base text-sf-muted md:text-lg">
              Compra tus productos, organiza el mercado y recibe ayuda cuando
              la necesites. Un supermercado digital, práctico y cercano.
            </p>
            <div className="mt-8 flex flex-col gap-3 md:flex-row">
              <Link href="/catalog" className={buttonClassName("primary")}>
                Ver productos
              </Link>
              <Link href="/search" className={buttonClassName("secondary")}>
                Buscar productos
              </Link>
            </div>
          </div>
        </Container>
      </section>

      <section>
        <Container className="grid gap-4 py-12 md:grid-cols-3 md:gap-6 md:py-16">
          {quickAccess.map((item) => {
            const Icon = item.icon;
            return (
              <Link key={item.href} href={item.href} className="group block">
                <Card className="h-full transition-colors group-hover:border-sf-primary/30">
                  <span className="inline-flex size-10 items-center justify-center rounded-lg bg-sf-bg text-sf-primary">
                    <Icon />
                  </span>
                  <h2 className="mt-4 text-xl font-semibold text-sf-ink">
                    {item.title}
                  </h2>
                  <p className="mt-2 text-sm text-sf-muted">{item.description}</p>
                </Card>
              </Link>
            );
          })}
        </Container>
      </section>

      <section>
        <Container className="pb-12 md:pb-16">
          <div className="mb-6 flex items-end justify-between gap-4">
            <div>
              <h2 className="text-[1.5rem] font-bold text-sf-ink md:text-[1.875rem]">
                Productos
              </h2>
              <p className="mt-2 text-sf-muted">
                Lo que puedes llevar a casa hoy.
              </p>
            </div>
          </div>
          <HomeProductPreview />
        </Container>
      </section>

      <section className="pb-16 md:pb-24">
        <Container>
          <Card className="border-sf-yellow/60 bg-sf-yellow-soft">
            <Badge tone="accent">Fercho</Badge>
            <h2 className="mt-4 text-[1.5rem] font-bold text-sf-ink md:text-[1.875rem]">
              Tu asistente en el súper
            </h2>
            <p className="mt-3 max-w-2xl text-base text-sf-muted">
              Fercho es el asistente inteligente de SuperFercho. El chat aún no
              está disponible en esta versión: por ahora puedes comprar desde el
              catálogo, el carrito y tus listas.
            </p>
          </Card>
        </Container>
      </section>
    </main>
  );
}
