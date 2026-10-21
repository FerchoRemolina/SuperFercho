"use client";

import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { CheckCircleIcon, ChevronIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

/**
 * Botón-filtro de tienda para el catálogo Customer: muestra nombre, estado y
 * valor seleccionado; abre un panel flotante que nunca altera el layout.
 * El cierre por click-fuera/Escape sigue el patrón de los menús del proyecto.
 */
export function CatalogFilterDropdown({
  label,
  valueLabel,
  active,
  alwaysShowValue = false,
  onOpenChange,
  children,
}: {
  label: string;
  /** Valor seleccionado actual (se muestra en el trigger); null = sin selección. */
  valueLabel?: string | null;
  active?: boolean;
  /** Mostrar el valor incluso sin filtro activo (p. ej. "Ordenar por: Relevancia"). */
  alwaysShowValue?: boolean;
  /** Notifica apertura/cierre del panel (p. ej. para reiniciar borradores al abrir). */
  onOpenChange?: (open: boolean) => void;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const panelId = useId();

  function setOpenAndNotify(next: boolean) {
    setOpen(next);
    onOpenChange?.(next);
  }

  useEffect(() => {
    if (!open) {
      return;
    }
    function closePanel() {
      setOpen(false);
      onOpenChange?.(false);
    }
    function onPointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        closePanel();
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        closePanel();
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open, onOpenChange]);

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpenAndNotify(!open)}
        className={cx(
          "inline-flex min-h-9 items-center gap-1.5 rounded-full border px-3.5 text-[13px] font-semibold",
          "shadow-sm transition-colors duration-150",
          active
            ? "border-sf-primary/50 bg-sf-primary-soft text-sf-primary"
            : "border-sf-border bg-sf-surface text-sf-ink hover:border-sf-primary/40",
        )}
      >
        <span>
          {label}
          {valueLabel && (active || alwaysShowValue) ? (
            <span className="font-semibold">: {valueLabel}</span>
          ) : null}
        </span>
        <ChevronIcon
          aria-hidden="true"
          className={cx(
            "h-3.5 w-3.5 shrink-0 transition-transform duration-150 motion-reduce:transition-none",
            open ? "-rotate-180 opacity-90" : "opacity-60",
          )}
        />
      </button>
      {open ? (
        <div
          id={panelId}
          className={cx(
            "absolute left-0 top-full z-40 mt-2 rounded-xl border border-sf-border bg-sf-surface p-2",
            "shadow-[0_12px_32px_rgba(23,33,27,0.14)]",
          )}
        >
          {children}
        </div>
      ) : null}
    </div>
  );
}

/** Lista de opciones dentro de un panel de filtro (una seleccionable + "todos"). */
export function CatalogFilterMenuList({
  options,
  value,
  allLabel,
  onSelect,
}: {
  options: ReadonlyArray<{ value: string; label: string }>;
  value: string | null;
  allLabel: string;
  onSelect: (value: string | null) => void;
}) {
  return (
    <ul role="listbox" aria-label={allLabel} className="grid min-w-52 gap-1">
      <li>
        <FilterMenuOption
          selected={value === null}
          onSelect={() => onSelect(null)}
        >
          {allLabel}
        </FilterMenuOption>
      </li>
      {options.map((option) => (
        <li key={option.value}>
          <FilterMenuOption
            selected={value === option.value}
            onSelect={() => onSelect(option.value)}
          >
            {option.label}
          </FilterMenuOption>
        </li>
      ))}
    </ul>
  );
}

function FilterMenuOption({
  selected,
  onSelect,
  children,
}: {
  selected: boolean;
  onSelect: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={selected}
      onClick={onSelect}
      className={cx(
        "flex min-h-10 w-full items-center justify-between gap-3 rounded-lg px-3 text-left text-sm transition-colors",
        selected
          ? "bg-sf-primary-soft font-semibold text-sf-primary"
          : "font-medium text-sf-ink hover:bg-sf-bg",
      )}
    >
      <span className="min-w-0">{children}</span>
      {selected ? (
        <CheckCircleIcon className="h-4 w-4 shrink-0" aria-hidden="true" />
      ) : null}
    </button>
  );
}
