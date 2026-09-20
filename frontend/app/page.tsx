import Link from "next/link";

export default function HomePage() {
  return (
    <main className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-3xl font-semibold tracking-tight">SuperFercho</h1>
      <p className="mt-3 text-sf-muted">
        Fundación técnica del frontend. El catálogo, la cuenta, el carrito y el
        asistente se implementarán en fases posteriores. Esta aplicación no
        duplica reglas de negocio del backend.
      </p>
      <ul className="mt-8 grid gap-3 sm:grid-cols-2">
        <li>
          <Link
            href="/catalog"
            className="block rounded-md border border-sf-border bg-sf-surface px-4 py-3 hover:border-sf-accent"
          >
            Catálogo (cascarón)
          </Link>
        </li>
        <li>
          <Link
            href="/login"
            className="block rounded-md border border-sf-border bg-sf-surface px-4 py-3 hover:border-sf-accent"
          >
            Acceso (cascarón)
          </Link>
        </li>
        <li>
          <Link
            href="/admin"
            className="block rounded-md border border-sf-border bg-sf-surface px-4 py-3 hover:border-sf-accent"
          >
            Admin (cascarón)
          </Link>
        </li>
        <li>
          <Link
            href="/assistant"
            className="block rounded-md border border-sf-border bg-sf-surface px-4 py-3 hover:border-sf-accent"
          >
            Asistente (cascarón)
          </Link>
        </li>
      </ul>
    </main>
  );
}
