"use client";

import Link from "next/link";
import { useSession } from "@/shared/session/session-provider";
import { HeaderIconAction } from "@/shared/ui/header-icon-action";
import { buttonClassName } from "@/shared/ui/button";
import { HeartIcon } from "@/shared/ui/icons";

/**
 * Acceso a Favoritos en el encabezado: únicamente icono (sin contador),
 * círculo con etiqueta según la referencia aprobada.
 */
export function FavoritesHeaderLink({ variant = "compact" }: { variant?: "compact" | "header" }) {
  const { session } = useSession();

  if (session?.role !== "CUSTOMER") {
    return null;
  }

  if (variant === "header") {
    return (
      <HeaderIconAction
        label="Favoritos"
        icon={<HeartIcon width={24} height={24} strokeWidth={2} />}
        href="/favorites"
      />
    );
  }

  return (
    <Link
      href="/favorites"
      aria-label="Favoritos"
      title="Favoritos"
      className={buttonClassName("ghost", "px-3")}
    >
      <HeartIcon />
    </Link>
  );
}
