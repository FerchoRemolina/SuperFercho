import Link from "next/link";
import { buttonClassName } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";

export default function NotFound() {
  return (
    <Container as="main" className="flex min-h-[50vh] flex-col items-center justify-center py-16 text-center">
      <p
        aria-hidden="true"
        className="bg-gradient-to-r from-sf-primary to-sf-primary/60 bg-clip-text text-6xl font-bold tracking-tight text-transparent md:text-7xl"
      >
        404
      </p>
      <h1 className="mt-4 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
        No encontramos esta página
      </h1>
      <p className="mt-2 max-w-md text-sm text-sf-muted md:text-base">
        Puede que el enlace esté desactualizado o que la página se haya movido.
      </p>
      <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
        <Link href="/" className={buttonClassName("primary")}>
          Ir al inicio
        </Link>
        <Link href="/catalog" className={buttonClassName("secondary")}>
          Ver catálogo
        </Link>
      </div>
    </Container>
  );
}
