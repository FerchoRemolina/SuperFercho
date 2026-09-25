import type { ComponentType, SVGProps } from "react";
import Link from "next/link";
import { AdminStockAttentionPanel } from "@/features/admin/components/admin-stock-attention-panel";
import {
  HubCategoriesArt,
  HubKnowledgeArt,
  HubOrdersArt,
  HubProductsArt,
} from "@/features/admin/components/admin-hub-art";
import { Container } from "@/shared/ui/container";
import {
  BookIcon,
  ChevronIcon,
  ClipboardListIcon,
  PackageIcon,
  TagIcon,
} from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

type IconComponent = ComponentType<SVGProps<SVGSVGElement>>;
type ArtComponent = ComponentType<SVGProps<SVGSVGElement>>;

type HubTone = "green" | "blue" | "purple" | "amber";

const sections: {
  href: string;
  title: string;
  description: string;
  cta: string;
  icon: IconComponent;
  art: ArtComponent;
  tone: HubTone;
}[] = [
  {
    href: "/admin/products",
    title: "Productos",
    description:
      "Consulta, crea y actualiza el catálogo. Ajusta el stock desde el detalle del producto.",
    cta: "Ir a productos",
    icon: PackageIcon,
    art: HubProductsArt,
    tone: "green",
  },
  {
    href: "/admin/categories",
    title: "Categorías",
    description:
      "Crea y actualiza categorías, y actívalas o desactívalas según el catálogo.",
    cta: "Ir a categorías",
    icon: TagIcon,
    art: HubCategoriesArt,
    tone: "blue",
  },
  {
    href: "/admin/orders",
    title: "Pedidos",
    description:
      "Consulta los pedidos de todos los clientes y filtra por estado.",
    cta: "Ir a pedidos",
    icon: ClipboardListIcon,
    art: HubOrdersArt,
    tone: "purple",
  },
  {
    href: "/admin/knowledge",
    title: "Base de conocimiento",
    description:
      "Consulta y gestiona los documentos de ayuda del supermercado.",
    cta: "Ir a base de conocimiento",
    icon: BookIcon,
    art: HubKnowledgeArt,
    tone: "amber",
  },
];

const toneStyles: Record<
  HubTone,
  {
    card: string;
    icon: string;
    title: string;
    cta: string;
    art: string;
  }
> = {
  green: {
    card: "border-emerald-200/80 bg-emerald-50/80 hover:border-emerald-300 hover:bg-emerald-50",
    icon: "bg-emerald-500 text-white shadow-sm shadow-emerald-600/20",
    title: "text-sf-ink group-hover:text-emerald-900",
    cta: "text-emerald-700",
    art: "text-emerald-600",
  },
  blue: {
    card: "border-sky-200/80 bg-sky-50/75 hover:border-sky-300 hover:bg-sky-50",
    icon: "bg-sky-500 text-white shadow-sm shadow-sky-600/20",
    title: "text-sf-ink group-hover:text-sky-900",
    cta: "text-sky-700",
    art: "text-sky-600",
  },
  purple: {
    card: "border-violet-200/80 bg-violet-50/70 hover:border-violet-300 hover:bg-violet-50",
    icon: "bg-violet-500 text-white shadow-sm shadow-violet-600/20",
    title: "text-sf-ink group-hover:text-violet-900",
    cta: "text-violet-700",
    art: "text-violet-600",
  },
  amber: {
    card: "border-amber-200/80 bg-amber-50/80 hover:border-amber-300 hover:bg-amber-50",
    icon: "bg-amber-500 text-white shadow-sm shadow-amber-600/20",
    title: "text-sf-ink group-hover:text-amber-950",
    cta: "text-amber-700",
    art: "text-amber-600",
  },
};

export function AdminHub() {
  return (
    <Container as="main" className="py-8 md:py-12">
      <header
        className={cx(
          "rounded-3xl border border-emerald-100/90 bg-emerald-50/70",
          "px-5 py-6 shadow-[0_1px_2px_rgba(23,33,27,0.04)] md:px-8 md:py-7",
        )}
      >
        <h1 className="text-[1.85rem] font-bold tracking-tight text-sf-ink md:text-[2.35rem]">
          Panel de administración
        </h1>
      </header>

      <section className="mt-7 md:mt-8" aria-label="Secciones de administración">
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {sections.map((section) => {
            const Icon = section.icon;
            const Art = section.art;
            const styles = toneStyles[section.tone];
            return (
              <li key={section.href}>
                <Link
                  href={section.href}
                  className={cx(
                    "group relative flex h-full min-h-[15.5rem] flex-col overflow-hidden rounded-2xl border p-5",
                    "shadow-[0_2px_8px_rgba(23,33,27,0.04)] transition-all duration-200",
                    "hover:-translate-y-0.5 hover:shadow-[0_12px_28px_rgba(23,33,27,0.08)]",
                    "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
                    styles.card,
                    "md:min-h-[16.5rem] md:p-5",
                  )}
                >
                  <div className="relative z-10 flex items-start justify-between gap-3">
                    <span
                      className={cx(
                        "flex h-12 w-12 items-center justify-center rounded-2xl transition-transform duration-200 group-hover:scale-105",
                        styles.icon,
                      )}
                      aria-hidden="true"
                    >
                      <Icon className="h-6 w-6" />
                    </span>
                    <ChevronIcon
                      className={cx(
                        "mt-1 h-4 w-4 opacity-45 transition-all duration-200",
                        "group-hover:translate-x-0.5 group-hover:opacity-100",
                        styles.cta,
                      )}
                    />
                  </div>

                  <div className="relative z-10 mt-4 max-w-[13.5rem] sm:max-w-[14rem] xl:max-w-[11.5rem]">
                    <h2
                      className={cx(
                        "text-xl font-bold tracking-tight transition-colors",
                        styles.title,
                      )}
                    >
                      {section.title}
                    </h2>
                    <p className="mt-2 text-sm leading-relaxed text-sf-muted">
                      {section.description}
                    </p>
                  </div>

                  <span
                    className={cx(
                      "relative z-10 mt-auto inline-flex items-center gap-1 pt-5 text-sm font-semibold transition-transform duration-200",
                      "group-hover:translate-x-0.5",
                      styles.cta,
                    )}
                  >
                    {section.cta}
                    <ChevronIcon className="h-4 w-4" />
                  </span>

                  <span
                    className={cx(
                      "pointer-events-none absolute -bottom-1 -right-1 h-[6.75rem] w-[7.5rem] opacity-90",
                      "transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5",
                      "sm:h-[7.25rem] sm:w-[8rem]",
                      styles.art,
                    )}
                    aria-hidden="true"
                  >
                    <Art />
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      </section>

      <AdminStockAttentionPanel />
    </Container>
  );
}
