import Link from "next/link";
import type { ReactNode } from "react";
import { ChevronIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

/** Navegación secundaria de retorno al listado, estandarizada para las
 *  vistas del Admin: azul del sistema, icono de flecha y hover suave.
 *  No es una acción primaria. */
export function BackLink({
  href,
  children = "Volver al listado",
  className,
}: {
  href: string;
  children?: ReactNode;
  className?: string;
}) {
  return (
    <Link
      href={href}
      className={cx(
        "inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 -mx-2.5",
        "text-[15px] font-medium text-sf-primary transition-colors hover:bg-sf-primary/10",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
        className,
      )}
    >
      <ChevronIcon className="h-4 w-4 shrink-0 -rotate-90" />
      {children}
    </Link>
  );
}
