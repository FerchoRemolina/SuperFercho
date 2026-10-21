import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("search view presentation", () => {
  const searchBar = source("features/catalog/components/search-bar.tsx");
  const searchPage = source("features/catalog/components/search-page-content.tsx");

  it("integrates the focus indicator into the pill without inner rectangular borders", () => {
    // El contenedor comunica el foco con borde + ring verde de marca.
    expect(searchBar).toContain("focus-within:border-sf-primary");
    expect(searchBar).toContain("focus-within:ring-2");
    // El campo no dibuja su propio contorno ni el botón nativo de limpieza.
    expect(searchBar).toContain("focus-visible:outline-none");
    expect(searchBar).toContain("[&::-webkit-search-cancel-button]:hidden");
    // Altura cómoda y alineación vertical de icono, campo y botón.
    expect(searchBar).toContain("items-center");
    expect(searchBar).toContain("min-h-10");
  });

  it("keeps functional search copy and states untouched", () => {
    expect(searchPage).toContain("Escribe para buscar");
    expect(searchPage).toContain("Usa el nombre, la marca o el código del producto.");
    expect(searchPage).toContain("No encontramos productos.");
    expect(searchPage).toContain("Prueba con otro término");
    expect(searchPage).toContain('"/catalog"');
    expect(searchPage).toContain("<SearchBar");
    expect(searchPage).toContain("<ProductGrid");
  });
});

describe("catalog customer presentation", () => {
  it("keeps category chip hrefs while enabling compact horizontal scroll on mobile", () => {
    const chips = source("features/catalog/components/category-chips.tsx");
    expect(chips).toContain('href="/catalog"');
    expect(chips).toContain("`/categories/${category.id}`");
    expect(chips).toContain("overflow-x-auto");
    expect(chips).toContain("md:flex-wrap");
    expect(chips).toContain("max-w-[12rem]");
  });

  it("compacts quantity selector chrome without removing touch targets", () => {
    const selector = source(
      "features/catalog/components/product-quantity-selector.tsx",
    );
    expect(selector).toContain("min-h-11");
    expect(selector).toContain("justify-center");
    expect(selector).toContain("gap-1.5");
  });

  it("derives hierarchical filters from real product data without hardcoded taxonomy maps", () => {
    const browse = source("features/catalog/components/catalog-browse.tsx");
    const filters = source("features/catalog/catalog-filters.ts");

    // No existen mapas temporales ni hardcodeados de taxonomía.
    expect(browse).not.toContain("PRODUCT_TYPE_NAMES");
    expect(browse).not.toContain("PRODUCT_VARIANT_NAMES");

    // Las opciones se derivan de los campos públicos reales del backend.
    expect(filters).toContain("productTypeName");
    expect(filters).toContain("productVariantName");
    expect(browse).toContain("collectProductTypeOptions");
    expect(browse).toContain("collectProductVariantOptions");
    expect(browse).toContain("collectPresentationOptions");

    // La cascada aprobada: el tipo invalida variante y presentación;
    // la variante invalida la presentación.
    expect(filters).toContain("withProductType");
    expect(filters).toContain("withProductVariant");
    expect(browse).toContain("withProductType(current, value)");
    expect(browse).toContain("withProductVariant(current, value)");
  });

  it("keeps relevance ordering and shows no visible sort control", () => {
    const browse = source("features/catalog/components/catalog-browse.tsx");
    const filters = source("features/catalog/catalog-filters.ts");

    // Sin control «Ordenar por» en la interfaz; el listado conserva el orden
    // del catálogo (relevancia) y la capacidad queda como default documentado.
    expect(browse).not.toContain("Ordenar por");
    expect(browse).not.toContain("sortProducts");
    expect(filters).toContain('"relevance"');
    expect(filters).toContain("sortProducts");
  });

  it("applies the price range with dual sliders only on Aplicar", () => {
    const price = source("features/catalog/components/catalog-price-filter.tsx");
    const browse = source("features/catalog/components/catalog-browse.tsx");

    expect(price.match(/type="range"/g)?.length).toBe(2);
    expect(price).toContain("Aplicar");
    expect(price).toContain("Limpiar");
    expect(price).toContain("accent-sf-primary");
    // Los límites provienen de los datos, no de un máximo fijo.
    expect(browse).toContain("priceRangeBounds(products)");
    // El chip de precio activo muestra el rango aplicado y es removible.
    expect(browse).toContain('label="Precio"');
  });
});
