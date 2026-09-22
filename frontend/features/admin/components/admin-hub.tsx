import Link from "next/link";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";

const sections = [
  {
    href: "/admin/products",
    title: "Productos",
    description: "Consulta, crea y actualiza el catálogo. El stock inicial se define al crear el producto.",
  },
  {
    href: "/admin/categories",
    title: "Categorías",
    description: "Crea y actualiza categorías, y actívalas o desactívalas según el catálogo.",
  },
];

export function AdminHub() {
  return (
    <Container as="main" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Administración
      </h1>
      <p className="mt-3 max-w-2xl text-base text-sf-muted">
        Gestiona el catálogo de SuperFercho. No hay métricas ni tablero de
        ventas en esta pantalla.
      </p>
      <ul className="mt-8 grid gap-4 md:grid-cols-2">
        {sections.map((section) => (
          <li key={section.href}>
            <Link href={section.href} className="block h-full">
              <Card className="h-full transition-colors hover:border-sf-primary">
                <h2 className="text-xl font-semibold text-sf-ink">{section.title}</h2>
                <p className="mt-2 text-sm text-sf-muted">{section.description}</p>
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </Container>
  );
}
