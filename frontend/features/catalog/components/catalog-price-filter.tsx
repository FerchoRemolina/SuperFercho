"use client";

import { useState, type FormEvent } from "react";
import { formatPriceFilterLabel } from "@/features/catalog/catalog-filters";
import { CatalogFilterDropdown } from "@/features/catalog/components/catalog-filter-dropdown";
import { Button } from "@/shared/ui/button";
import { formatMoney } from "@/shared/money/money";

/**
 * Filtro de precio por rango con dos deslizadores (mínimo y máximo) para el
 * catálogo Customer. Client-side sobre el listado ya cargado; sin parámetros
 * de backend. Los valores se ajustan como borrador y solo se aplican al
 * pulsar «Aplicar».
 */
export function CatalogPriceFilter({
  priceMin,
  priceMax,
  bounds,
  onChange,
}: {
  priceMin: number | null;
  priceMax: number | null;
  /** Límites derivados de los productos cargados; null = sin productos. */
  bounds: { min: number; max: number } | null;
  onChange: (min: number | null, max: number | null) => void;
}) {
  const active = priceMin !== null || priceMax !== null;
  const minBound = bounds?.min ?? 0;
  const maxBound = bounds?.max ?? minBound + 1000;
  const [draftMin, setDraftMin] = useState(
    priceMin === null ? minBound : Math.min(Math.max(priceMin, minBound), maxBound),
  );
  const [draftMax, setDraftMax] = useState(
    priceMax === null ? maxBound : Math.min(Math.max(priceMax, minBound), maxBound),
  );
  const [synced, setSynced] = useState<{ min: number | null; max: number | null }>({
    min: priceMin,
    max: priceMax,
  });

  function resetDrafts() {
    setDraftMin(priceMin === null ? minBound : priceMin);
    setDraftMax(priceMax === null ? maxBound : priceMax);
  }

  // Si el rango aplicado cambia desde fuera (p. ej. «Quitar filtros»),
  // el borrador se sincroniza (patrón de ajuste en render del proyecto).
  if (synced.min !== priceMin || synced.max !== priceMax) {
    setSynced({ min: priceMin, max: priceMax });
    resetDrafts();
  }

  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const min = Math.min(draftMin, draftMax);
    const max = Math.max(draftMin, draftMax);
    // Rango completo = sin filtro: se envía null en los extremos que tocan
    // los límites para no mostrar un chip de precio sin efecto real.
    onChange(min <= minBound ? null : min, max >= maxBound ? null : max);
  }

  return (
    <CatalogFilterDropdown
      label="Precio"
      valueLabel={active ? formatPriceFilterLabel(priceMin, priceMax) : null}
      active={active}
      onOpenChange={(open) => {
        if (open) {
          resetDrafts();
        }
      }}
    >
      <form
        className="grid min-w-64 gap-4 p-1.5"
        onSubmit={apply}
        aria-label="Filtrar por precio"
      >
        <label className="grid gap-1.5">
          <span className="flex items-center justify-between text-xs font-medium text-sf-muted">
            Mínimo
            <span className="text-sm font-semibold tabular-nums text-sf-ink">
              {formatMoney({ amount: draftMin, currency: "COP" })}
            </span>
          </span>
          <input
            type="range"
            min={minBound}
            max={maxBound}
            step={100}
            value={draftMin}
            onChange={(event) => {
              const next = Number.parseInt(event.target.value, 10);
              setDraftMin(next);
              // El mínimo nunca supera al máximo.
              if (next > draftMax) {
                setDraftMax(next);
              }
            }}
            className="accent-sf-primary"
            aria-label="Precio mínimo"
          />
        </label>
        <label className="grid gap-1.5">
          <span className="flex items-center justify-between text-xs font-medium text-sf-muted">
            Máximo
            <span className="text-sm font-semibold tabular-nums text-sf-ink">
              {formatMoney({ amount: draftMax, currency: "COP" })}
            </span>
          </span>
          <input
            type="range"
            min={minBound}
            max={maxBound}
            step={100}
            value={draftMax}
            onChange={(event) => {
              const next = Number.parseInt(event.target.value, 10);
              setDraftMax(next);
              // El máximo nunca es inferior al mínimo.
              if (next < draftMin) {
                setDraftMin(next);
              }
            }}
            className="accent-sf-primary"
            aria-label="Precio máximo"
          />
        </label>
        <div className="flex items-center justify-between gap-2">
          <button
            type="button"
            className="min-h-10 px-1 text-sm font-semibold text-sf-muted transition-colors hover:text-sf-error"
            onClick={() => {
              setDraftMin(minBound);
              setDraftMax(maxBound);
              onChange(null, null);
            }}
          >
            Limpiar
          </button>
          <Button type="submit" className="min-h-10 px-5 text-sm">
            Aplicar
          </Button>
        </div>
      </form>
    </CatalogFilterDropdown>
  );
}
