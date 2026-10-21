import Link from "next/link";
import type { ReactNode } from "react";
import { Container } from "@/shared/ui/container";
import { cx } from "@/shared/utils/cx";

export function LegalDocument({
  title,
  updatedAt,
  intro,
  other: { href, label },
  children,
}: {
  title: string;
  updatedAt: string;
  intro: string;
  other: { href: string; label: string };
  children: ReactNode;
}) {
  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <Link
        href="/"
        className={cx(
          "inline-flex min-h-9 items-center gap-1.5 rounded-full border border-sf-border bg-sf-surface px-3.5",
          "text-sm font-semibold text-sf-muted transition-colors duration-150",
          "hover:border-sf-primary/40 hover:text-sf-primary",
        )}
      >
        ← Tienda
      </Link>
      <article className="mx-auto mt-6 max-w-2xl md:mt-8">
        <h1 className="text-[1.75rem] font-bold leading-tight tracking-tight text-sf-ink md:text-4xl">
          {title}
        </h1>
        <p className="mt-2 text-xs font-medium uppercase tracking-[0.08em] text-sf-muted">
          Última actualización: {updatedAt}
        </p>
        <p className="mt-4 text-sm leading-relaxed text-sf-muted md:text-base">
          {intro}
        </p>
        <div className="mt-8 grid gap-8 md:mt-10">{children}</div>
        <div className="mt-10 grid gap-2 rounded-xl border border-sf-border bg-sf-bg p-4 text-sm text-sf-muted md:grid-cols-2">
          <p>
            Documento de ejemplo versión inicial, pendiente de revisión legal y
            de la información del responsable antes de su publicación
            definitiva.
          </p>
          <Link
            href={href}
            className="min-h-11 self-center font-semibold text-sf-primary hover:underline md:justify-self-end"
          >
            {label} →
          </Link>
        </div>
      </article>
    </Container>
  );
}

export function LegalSection({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="grid gap-2.5">
      <h2 className="text-lg font-semibold text-sf-ink md:text-xl">{title}</h2>
      {children}
    </section>
  );
}

export function LegalParagraph({ children }: { children: ReactNode }) {
  return <p className="text-sm leading-relaxed text-sf-ink md:text-base">{children}</p>;
}

export function LegalList({ children }: { children: ReactNode }) {
  return (
    <ul className="grid list-disc gap-1.5 pl-5 text-sm leading-relaxed text-sf-ink md:text-base">
      {children}
    </ul>
  );
}
