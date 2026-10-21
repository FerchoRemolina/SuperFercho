"use client";

import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  applyProductCatalogFilters,
  collectPresentationOptions,
  collectProductTypeOptions,
  collectProductVariantOptions,
  EMPTY_PRODUCT_CATALOG_FILTERS,
  hasActiveProductFilters,
  parsePriceInput,
  priceRangeBounds,
  withCategory,
  withProductType,
  withProductVariant,
  type ProductCatalogFilters,
} from "@/features/catalog/catalog-filters";
import { CatalogFilterDropdown } from "@/features/catalog/components/catalog-filter-dropdown";
import { CatalogFilterMenuList } from "@/features/catalog/components/catalog-filter-dropdown";
import { CatalogPriceFilter } from "@/features/catalog/components/catalog-price-filter";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import {
  ProductGrid,
  ProductGridSkeleton,
} from "@/features/catalog/components/product-grid";
import { SearchBar } from "@/features/catalog/components/search-bar";
import {
  useCategoriesQuery,
  useSearchProductsQuery,
} from "@/features/catalog/hooks";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { SearchIcon } from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { buttonClassName } from "@/shared/ui/button";

/** Estado de filtros de la búsqueda, representado íntegramente en la URL. */
function readFilters(searchParams: {
  get(name: string): string | null;
}): ProductCatalogFilters {
  return {
    categoryId: searchParams.get("categoryId"),
    productTypeId: searchParams.get("productTypeId"),
    productVariantId: searchParams.get("productVariantId"),
    presentationKey: searchParams.get("presentation"),
    priceMin: parsePriceInput(searchParams.get("priceMin") ?? ""),
    priceMax: parsePriceInput(searchParams.get("priceMax") ?? ""),
  };
}

export function SearchPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const text = (searchParams.get("text") ?? "").trim();
  const searchQuery = useSearchProductsQuery(text);
  // El estado de filtros vive en la URL (compartible y sobrevive recargas).
  const filters = readFilters(searchParams);

  const categoriesQuery = useCategoriesQuery();
  const results = searchQuery.data ?? [];
  const visibleProducts = applyProductCatalogFilters(results, filters);
  const filtersActive = hasActiveProductFilters(filters);
  const priceBounds = priceRangeBounds(results);
  const typeOptions = collectProductTypeOptions(results);
  const variantOptions = collectProductVariantOptions(
    results,
    filters.productTypeId,
  );
  const presentationOptions = collectPresentationOptions(
    results,
    filters.productTypeId,
    filters.productVariantId,
  );
  const hasResults = searchQuery.isSuccess && results.length > 0;
  const typeLabel = typeOptions.find(
    (option) => option.value === filters.productTypeId,
  )?.label;
  const variantLabel = variantOptions.find(
    (option) => option.value === filters.productVariantId,
  )?.label;
  const presentationLabel = presentationOptions.find(
    (option) => option.value === filters.presentationKey,
  )?.label;
  const categoryOptions =
    categoriesQuery.data
      ?.filter((category) => category.status === "ACTIVE")
      .map((category) => ({ value: category.id, label: category.name })) ?? [];
  const categoryLabel = categoryOptions.find(
    (option) => option.value === filters.categoryId,
  )?.label;

  function applyFilters(next: ProductCatalogFilters) {
    const params = new URLSearchParams();
    if (text) {
      params.set("text", text);
    }
    if (next.categoryId) {
      params.set("categoryId", next.categoryId);
    }
    if (next.productTypeId) {
      params.set("productTypeId", next.productTypeId);
    }
    if (next.productVariantId) {
      params.set("productVariantId", next.productVariantId);
    }
    if (next.presentationKey) {
      params.set("presentation", next.presentationKey);
    }
    if (next.priceMin !== null) {
      params.set("priceMin", String(next.priceMin));
    }
    if (next.priceMax !== null) {
      params.set("priceMax", String(next.priceMax));
    }
    const query = params.toString();
    router.replace(query ? `/search?${query}` : "/search", { scroll: false });
  }

  return (
    <Container as="main" width="wide" className="py-6 md:py-8">
      <PageHeader
        title={text ? `Resultados para “${text}”` : "Buscar"}
        description={
          text
            ? "Encontramos productos que coinciden con tu búsqueda."
            : "Encuentra productos por nombre, marca o código."
        }
        back={{ href: "/catalog", label: "← Catálogo" }}
      />

      <div className="mt-5 md:mt-6">
        <SearchBar key={text} initialText={text} autoFocus={!text} />
      </div>

      <div className="mt-5 md:mt-6">
        {!text ? (
          <EmptyState
            title="Escribe para buscar"
            description="Usa el nombre, la marca o el código del producto."
            align="center"
            className="py-12"
            icon={<SearchIcon className="size-6" />}
          />
        ) : searchQuery.isPending ? (
          <ProductGridSkeleton />
        ) : searchQuery.isError ? (
          <CatalogQueryError
            error={searchQuery.error}
            title="No se pudo buscar"
          />
        ) : hasResults ? (
          <>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              {categoryOptions.length > 0 ? (
                <CatalogFilterDropdown
                  label="Categoría"
                  valueLabel={categoryLabel}
                  active={filters.categoryId !== null}
                >
                  <CatalogFilterMenuList
                    options={categoryOptions}
                    value={filters.categoryId}
                    allLabel="Todas las categorías"
                    onSelect={(value) =>
                      applyFilters(withCategory(filters, value))
                    }
                  />
                </CatalogFilterDropdown>
              ) : null}
              {typeOptions.length > 0 ? (
                <CatalogFilterDropdown
                  label="Tipo de producto"
                  valueLabel={typeLabel}
                  active={filters.productTypeId !== null}
                >
                  <CatalogFilterMenuList
                    options={typeOptions}
                    value={filters.productTypeId}
                    allLabel="Todos los tipos"
                    onSelect={(value) =>
                      applyFilters(withProductType(filters, value))
                    }
                  />
                </CatalogFilterDropdown>
              ) : null}
              {variantOptions.length > 0 ? (
                <CatalogFilterDropdown
                  label="Versión"
                  valueLabel={variantLabel}
                  active={filters.productVariantId !== null}
                >
                  <CatalogFilterMenuList
                    options={variantOptions}
                    value={filters.productVariantId}
                    allLabel="Todas las variantes"
                    onSelect={(value) =>
                      applyFilters(withProductVariant(filters, value))
                    }
                  />
                </CatalogFilterDropdown>
              ) : null}
              {presentationOptions.length > 0 ? (
                <CatalogFilterDropdown
                  label="Presentación"
                  valueLabel={presentationLabel}
                  active={filters.presentationKey !== null}
                >
                  <CatalogFilterMenuList
                    options={presentationOptions}
                    value={filters.presentationKey}
                    allLabel="Todas las presentaciones"
                    onSelect={(value) =>
                      applyFilters({
                        ...filters,
                        presentationKey: value,
                      })
                    }
                  />
                </CatalogFilterDropdown>
              ) : null}
              {priceBounds ? (
                <CatalogPriceFilter
                  priceMin={filters.priceMin}
                  priceMax={filters.priceMax}
                  bounds={priceBounds}
                  onChange={(priceMin, priceMax) =>
                    applyFilters({ ...filters, priceMin, priceMax })
                  }
                />
              ) : null}
              {filtersActive ? (
                <button
                  type="button"
                  onClick={() => applyFilters(EMPTY_PRODUCT_CATALOG_FILTERS)}
                  className="min-h-9 px-2 text-[13px] font-semibold text-sf-primary transition-colors hover:underline"
                >
                  Quitar filtros
                </button>
              ) : null}
            </div>
            {visibleProducts.length > 0 ? (
              <ProductGrid products={visibleProducts} />
            ) : (
              <EmptyState
                title="No encontramos productos con esos filtros."
                description="Prueba quitando algún filtro o ajustando tu búsqueda."
                align="center"
                className="py-12"
                icon={<SearchIcon className="size-6" />}
                action={
                  <button
                    type="button"
                    className="text-sm font-semibold text-sf-primary hover:underline"
                    onClick={() => applyFilters(EMPTY_PRODUCT_CATALOG_FILTERS)}
                  >
                    Quitar filtros
                  </button>
                }
              />
            )}
          </>
        ) : (
          <EmptyState
            title="No encontramos productos."
            description={`No hay resultados para “${text}”. Prueba con otro término.`}
            align="center"
            className="py-12"
            icon={<SearchIcon className="size-6" />}
            action={
              <Link
                href="/catalog"
                className={buttonClassName("secondary", "min-h-10 px-5")}
              >
                Explorar el catálogo
              </Link>
            }
          />
        )}
      </div>
    </Container>
  );
}
