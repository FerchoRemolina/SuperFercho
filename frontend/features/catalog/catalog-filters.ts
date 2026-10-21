import type {
  Presentation,
  PresentationUnit,
  Product,
} from "@/features/catalog/api";
import { formatMoney } from "@/shared/money/money";

/**
 * Lógica pura de filtrado jerárquico del catálogo Customer.
 * La fuente de verdad son los productos ya cargados (GET /api/v1/products
 * público, que expone productTypeName / productVariantName / presentation);
 * no existen mapas ni nombres hardcodeados y no se inventan relaciones.
 *
 * Jerarquía: Categoría (chips, fuera de este módulo) → Tipo → Variante →
 * Presentación. Toda combinación de filtros se resuelve con AND sobre los
 * productos reales: una combinación inexistente simplemente devuelve cero
 * resultados.
 */

export type ProductSortOption = "relevance" | "price-asc" | "price-desc";

export const PRODUCT_SORT_OPTIONS: ReadonlyArray<{
  value: ProductSortOption;
  label: string;
}> = [
  { value: "relevance", label: "Relevancia" },
  { value: "price-asc", label: "Precio: menor a mayor" },
  { value: "price-desc", label: "Precio: mayor a menor" },
];

export type ProductCatalogFilters = {
  /** Filtro client-side por categoría (búsqueda global: primer contexto). */
  categoryId: string | null;
  productTypeId: string | null;
  productVariantId: string | null;
  /**
   * Clave "quantity:unit" derivada de los datos cargados. No es un id de
   * dominio: la presentación se identifica por su cantidad y unidad.
   */
  presentationKey: string | null;
  /** Precio mínimo en COP (inclusivo). null = sin límite. */
  priceMin: number | null;
  /** Precio máximo en COP (inclusivo). null = sin límite. */
  priceMax: number | null;
};

export const EMPTY_PRODUCT_CATALOG_FILTERS: ProductCatalogFilters = {
  categoryId: null,
  productTypeId: null,
  productVariantId: null,
  presentationKey: null,
  priceMin: null,
  priceMax: null,
};

export type FilterOption = {
  value: string;
  label: string;
};

export function sortProducts(
  products: Product[],
  sort: ProductSortOption,
): Product[] {
  const sorted = [...products];
  switch (sort) {
    case "price-asc":
      sorted.sort((left, right) => left.price.amount - right.price.amount);
      break;
    case "price-desc":
      sorted.sort((left, right) => right.price.amount - left.price.amount);
      break;
    default:
      break;
  }
  return sorted;
}

/**
 * Tipos de producto presentes en los productos cargados, etiquetados con su
 * nombre comercial real (productTypeName). Sin nombre no hay opción: no se
 * inventan fallbacks ni se exponen identificadores técnicos.
 */
export function collectProductTypeOptions(products: Product[]): FilterOption[] {
  const namesById = new Map<string, string>();
  for (const product of products) {
    if (
      product.productTypeId &&
      product.productTypeName &&
      !namesById.has(product.productTypeId)
    ) {
      namesById.set(product.productTypeId, product.productTypeName);
    }
  }
  return [...namesById].map(([value, label]) => ({ value, label }));
}

/**
 * Variantes válidas dentro del tipo seleccionado (relación real de los datos:
 * cada producto trae su par productTypeId + productVariantId).
 */
export function collectProductVariantOptions(
  products: Product[],
  productTypeId: string | null,
): FilterOption[] {
  const namesById = new Map<string, string>();
  for (const product of products) {
    if (productTypeId !== null && product.productTypeId !== productTypeId) {
      continue;
    }
    if (
      product.productVariantId &&
      product.productVariantName &&
      !namesById.has(product.productVariantId)
    ) {
      namesById.set(product.productVariantId, product.productVariantName);
    }
  }
  return [...namesById].map(([value, label]) => ({ value, label }));
}

/** Orden de exhibit de unidades: masa, volumen y discretas (como el dominio). */
const PRESENTATION_UNIT_ORDER: ReadonlyArray<PresentationUnit> = [
  "KG",
  "G",
  "L",
  "ML",
  "UNIT",
  "PACK",
  "BOX",
  "ROLL",
];

export function presentationKeyOf(presentation: Presentation): string {
  return `${presentation.quantity}:${presentation.unit}`;
}

function parsePresentationKey(key: string): {
  quantity: number;
  unit: string;
} | null {
  const separator = key.indexOf(":");
  if (separator <= 0) {
    return null;
  }
  const quantity = Number(key.slice(0, separator));
  const unit = key.slice(separator + 1);
  if (!Number.isFinite(quantity) || unit === "") {
    return null;
  }
  return { quantity, unit };
}

const PRESENTATION_UNIT_LABELS: Record<
  PresentationUnit,
  { singular: string; plural: string | null }
> = {
  G: { singular: "g", plural: null },
  KG: { singular: "kg", plural: null },
  ML: { singular: "ml", plural: null },
  L: { singular: "L", plural: null },
  UNIT: { singular: "unidad", plural: "unidades" },
  PACK: { singular: "pack", plural: "packs" },
  BOX: { singular: "caja", plural: "cajas" },
  ROLL: { singular: "rollo", plural: "rollos" },
};

/** Lenguaje de tienda para presentaciones, derivado de quantity + unit. */
export function formatPresentationLabel(presentation: Presentation): string {
  const quantity = presentation.quantity.toLocaleString("es-CO");
  const unitLabel =
    PRESENTATION_UNIT_LABELS[presentation.unit] ?? { singular: presentation.unit, plural: null };
  if (unitLabel.plural !== null) {
    return `${quantity} ${presentation.quantity === 1 ? unitLabel.singular : unitLabel.plural}`;
  }
  return `${quantity} ${unitLabel.singular}`;
}

/**
 * Presentaciones válidas para la combinación tipo + variante seleccionada,
 * derivadas de los productos reales y ordenadas para exhibit.
 */
export function collectPresentationOptions(
  products: Product[],
  productTypeId: string | null,
  productVariantId: string | null,
): FilterOption[] {
  const presentationsByKey = new Map<string, Presentation>();
  for (const product of products) {
    if (productTypeId !== null && product.productTypeId !== productTypeId) {
      continue;
    }
    if (
      productVariantId !== null &&
      product.productVariantId !== productVariantId
    ) {
      continue;
    }
    if (!product.presentation) {
      continue;
    }
    const key = presentationKeyOf(product.presentation);
    if (!presentationsByKey.has(key)) {
      presentationsByKey.set(key, product.presentation);
    }
  }
  return [...presentationsByKey.entries()]
    .map(([value, presentation]) => ({
      value,
      label: formatPresentationLabel(presentation),
      presentation,
    }))
    .sort((left, right) => {
      const unitDelta =
        PRESENTATION_UNIT_ORDER.indexOf(left.presentation.unit) -
        PRESENTATION_UNIT_ORDER.indexOf(right.presentation.unit);
      if (unitDelta !== 0) {
        return unitDelta;
      }
      return right.presentation.quantity - left.presentation.quantity;
    })
    .map(({ value, label }) => ({ value, label }));
}

/**
 * Regla de cascada aprobada: cambiar la categoría invalida tipo, variante y
 * presentación; cambiar el tipo invalida variante y presentación; cambiar la
 * variante invalida la presentación.
 */
export function withCategory(
  current: ProductCatalogFilters,
  categoryId: string | null,
): ProductCatalogFilters {
  if (current.categoryId === categoryId) {
    return current;
  }
  return {
    ...current,
    categoryId,
    productTypeId: null,
    productVariantId: null,
    presentationKey: null,
  };
}

export function withProductType(
  current: ProductCatalogFilters,
  productTypeId: string | null,
): ProductCatalogFilters {
  if (current.productTypeId === productTypeId) {
    return current;
  }
  return { ...current, productTypeId, productVariantId: null, presentationKey: null };
}

export function withProductVariant(
  current: ProductCatalogFilters,
  productVariantId: string | null,
): ProductCatalogFilters {
  if (current.productVariantId === productVariantId) {
    return current;
  }
  return { ...current, productVariantId, presentationKey: null };
}

/** Convierte texto de input de precio COP en entero (o null si no hay valor). */
export function parsePriceInput(value: string): number | null {
  const digits = value.replace(/[^\d]/g, "");
  if (digits === "") {
    return null;
  }
  const parsed = Number.parseInt(digits, 10);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

/** Normaliza el par Desde/Hasta: si el mínimo supera el máximo, los intercambia. */
export function normalizePriceBounds(
  min: number | null,
  max: number | null,
): { min: number | null; max: number | null } {
  if (min !== null && max !== null && min > max) {
    return { min: max, max: min };
  }
  return { min, max };
}

/**
 * Límites del deslizador de precio, derivados de los productos cargados
 * (no existe un máximo global en el contrato). Mínimo redondeado hacia
 * abajo a $100 y máximo hacia arriba a $1.000. Si el listado no tiene
 * dispersión de precios, se reserva un rango de $1.000 para que el control
 * siga siendo usable (fallback documentado).
 */
export function priceRangeBounds(
  products: Product[],
): { min: number; max: number } | null {
  if (products.length === 0) {
    return null;
  }
  const amounts = products.map((product) => product.price.amount);
  const min = Math.floor(Math.min(...amounts) / 100) * 100;
  let max = Math.ceil(Math.max(...amounts) / 1000) * 1000;
  if (max <= min) {
    max = min + 1000;
  }
  return { min, max };
}

export function formatPriceFilterLabel(
  min: number | null,
  max: number | null,
): string {
  if (min !== null && max !== null) {
    return `${formatMoney({ amount: min, currency: "COP" })} – ${formatMoney({ amount: max, currency: "COP" })}`;
  }
  if (min !== null) {
    return `Desde ${formatMoney({ amount: min, currency: "COP" })}`;
  }
  if (max !== null) {
    return `Hasta ${formatMoney({ amount: max, currency: "COP" })}`;
  }
  return "Precio";
}

export function applyProductCatalogFilters(
  products: Product[],
  filters: ProductCatalogFilters,
): Product[] {
  let result = products;
  if (filters.categoryId !== null) {
    result = result.filter(
      (product) => product.categoryId === filters.categoryId,
    );
  }
  if (filters.productTypeId !== null) {
    result = result.filter(
      (product) => product.productTypeId === filters.productTypeId,
    );
  }
  if (filters.productVariantId !== null) {
    result = result.filter(
      (product) => product.productVariantId === filters.productVariantId,
    );
  }
  if (filters.presentationKey !== null) {
    const parsed = parsePresentationKey(filters.presentationKey);
    result = parsed
      ? result.filter(
          (product) =>
            product.presentation !== undefined &&
            product.presentation !== null &&
            product.presentation.quantity === parsed.quantity &&
            product.presentation.unit === parsed.unit,
        )
      : [];
  }
  if (filters.priceMin !== null) {
    result = result.filter((product) => product.price.amount >= filters.priceMin!);
  }
  if (filters.priceMax !== null) {
    result = result.filter((product) => product.price.amount <= filters.priceMax!);
  }
  return result;
}

export function hasActiveProductFilters(
  filters: ProductCatalogFilters,
): boolean {
  return (
    filters.productTypeId !== null ||
    filters.productVariantId !== null ||
    filters.presentationKey !== null ||
    filters.priceMin !== null ||
    filters.priceMax !== null
  );
}
