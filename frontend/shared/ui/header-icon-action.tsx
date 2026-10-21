import type { ReactNode } from "react";
import Link from "next/link";
import { cx } from "@/shared/utils/cx";

type HeaderIconActionProps = {
  label: string;
  icon: ReactNode;
  href?: string;
  onClick?: () => void;
  ariaExpanded?: boolean;
  ariaControls?: string;
  /** Contador numérico (solo Carrito). Favoritos no lleva contador. */
  badge?: number;
  active?: boolean;
};

/**
 * Acción de navegación del encabezado: únicamente icono dentro de un círculo
 * suave de 44×44. La etiqueta visible vive en `title` + `aria-label`;
 * `href` renderiza un enlace, sin `href` un botón (p. ej. Cuenta abre su menú).
 */
export function HeaderIconAction({
  label,
  icon,
  href,
  onClick,
  ariaExpanded,
  ariaControls,
  badge,
  active = false,
}: HeaderIconActionProps) {
  const actionClasses =
    "group flex h-11 w-11 items-center justify-center rounded-full focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary";
  const inner = (
    <span
      className={cx(
        "relative flex h-11 w-11 items-center justify-center rounded-full transition-colors duration-150",
        active
          ? "bg-sf-primary-soft ring-1 ring-sf-primary/30"
          : "bg-sf-bg/90 ring-1 ring-sf-border",
        "group-hover:bg-sf-primary-soft group-hover:ring-sf-primary/30 group-focus-visible:bg-sf-primary-soft",
      )}
    >
      {icon}
      {badge !== undefined && badge > 0 ? (
        <span className="absolute -right-1 -top-1 flex min-h-[18px] min-w-[18px] items-center justify-center rounded-full bg-sf-error px-1 text-[11px] font-bold leading-none text-white">
          {badge}
        </span>
      ) : null}
    </span>
  );
  if (href) {
    return (
      <Link
        href={href}
        aria-label={label}
        title={label}
        aria-current={active ? "page" : undefined}
        className={cx(actionClasses, "text-sf-primary")}
      >
        {inner}
      </Link>
    );
  }
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      aria-expanded={ariaExpanded}
      aria-controls={ariaControls}
      onClick={onClick}
      className={cx(actionClasses, "text-sf-primary")}
    >
      {inner}
    </button>
  );
}
