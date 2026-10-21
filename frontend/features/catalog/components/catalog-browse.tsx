"use client";

import { useState } from "react";
import {
  applyProductCatalogFilters,
  collectPresentationOptions,
  collectProductTypeOptions,
  collectProductVariantOptions,
  EMPTY_PRODUCT_CATALOG_FILTERS,
  formatPriceFilterLabel,
  hasActiveProductFilters,
  priceRangeBounds,
  withProductType,
  withProductVariant,
  type ProductCatalogFilters,
} from "@/features/catalog/catalog-filters";
import { CategoryChips } from "@/features/catalog/components/category-chips";
import { CatalogFilterDropdown } from "@/features/catalog/components/catalog-filter-dropdown";
import { CatalogFilterMenuList } from "@/features/catalog/components/catalog-filter-dropdown";
import { CatalogPriceFilter } from "@/features/catalog/components/catalog-price-filter";
import { CatalogQueryError } from "@/features/catalog/components/catalog-query-error";
import {
  ProductGrid,
  ProductGridSkeleton,
} from "@/features/catalog/components/product-grid";
import { useCategoriesQuery, useProductsQuery } from "@/features/catalog/hooks";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { CloseIcon, SearchIcon } from "@/shared/ui/icons";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";

export function CatalogBrowse({
  title,
  description,
  categoryId,
  emptyTitle,
  emptyDescription,
}: {
  title: string;
  description?: string;
  categoryId?: string;
  emptyTitle: string;
  emptyDescription: string;
}) {
  const categoriesQuery = useCategoriesQuery();
  const productsQuery = useProductsQuery(categoryId);

  const [filters, setFilters] = useState<ProductCatalogFilters>(
    EMPTY_PRODUCT_CATALOG_FILTERS,
  );
  const [syncedCategoryId, setSyncedCategoryId] = useState(categoryId);
  if (syncedCategoryId !== categoryId) {
    setSyncedCategoryId(categoryId);
    setFilters(EMPTY_PRODUCT_CATALOG_FILTERS);
  }

  const products = productsQuery.data ?? [];
  // Relevancia: los productos se muestran en el orden del catálogo (API);
  // no existe control de ordenamiento visible.
  const filtersActive = hasActiveProductFilters(filters);
  const visibleProducts = applyProductCatalogFilters(products, filters);

  // Opciones jerárquicas derivadas de los productos reales cargados
  // (la API pública entrega productTypeName / productVariantName / presentation):
  // sin mapas hardcodeados y sin nombres de fallback.
  const typeOptions = collectProductTypeOptions(products);
  const variantOptions = collectProductVariantOptions(
    products,
    filters.productTypeId,
  );
  const presentationOptions = collectPresentationOptions(
    products,
    filters.productTypeId,
    filters.productVariantId,
  );
  const priceBounds = priceRangeBounds(products);
  const selectedTypeLabel =
    typeOptions.find((option) => option.value === filters.productTypeId)?.label ??
    null;
  const selectedVariantLabel =
    variantOptions.find((option) => option.value === filters.productVariantId)
      ?.label ?? null;
  const selectedPresentationLabel =
    presentationOptions.find(
      (option) => option.value === filters.presentationKey,
    )?.label ?? null;
  const priceActive = filters.priceMin !== null || filters.priceMax !== null;

  return (
    <Container as="main" width="wide" className="py-6 md:py-8">
      <PageHeader title={title} description={description} />

      <div className="mt-4 md:mt-5">
        {categoriesQuery.isPending ? (
          <div className="flex flex-wrap gap-2" aria-hidden="true">
            <Skeleton className="h-11 w-20 rounded-full" />
            <Skeleton className="h-11 w-28 rounded-full" />
            <Skeleton className="h-11 w-24 rounded-full" />
          </div>
        ) : categoriesQuery.isError ? (
          <CatalogQueryError error={categoriesQuery.error} title="No se pudieron cargar las categorías" />
        ) : categoriesQuery.data && categoriesQuery.data.length > 0 ? (
          <CategoryChips
            categories={categoriesQuery.data}
            activeCategoryId={categoryId}
          />
        ) : null}
      </div>

      {productsQuery.isSuccess && products.length > 0 ? (
        <>
          <div className="mt-2.5 flex flex-wrap items-center gap-2 md:mt-3">
            <span
              aria-hidden="true"
              className="hidden text-xs font-semibold uppercase tracking-[0.06em] text-sf-muted sm:inline"
            >
              Filtros
            </span>
            {typeOptions.length > 0 ? (
              <CatalogFilterDropdown
                label="Tipo de producto"
                valueLabel={selectedTypeLabel}
                active={filters.productTypeId !== null}
              >
                <CatalogFilterMenuList
                  options={typeOptions}
                  value={filters.productTypeId}
                  allLabel="Todos los tipos"
                  onSelect={(value) =>
                    setFilters((current) => withProductType(current, value))
                  }
                />
              </CatalogFilterDropdown>
            ) : null}
            {variantOptions.length > 0 ? (
              <CatalogFilterDropdown
                label="Versión"
                valueLabel={selectedVariantLabel}
                active={filters.productVariantId !== null}
              >
                <CatalogFilterMenuList
                  options={variantOptions}
                  value={filters.productVariantId}
                  allLabel="Todas las variantes"
                  onSelect={(value) =>
                    setFilters((current) => withProductVariant(current, value))
                  }
                />
              </CatalogFilterDropdown>
            ) : null}
            {presentationOptions.length > 0 ? (
              <CatalogFilterDropdown
                label="Presentación"
                valueLabel={selectedPresentationLabel}
                active={filters.presentationKey !== null}
              >
                <CatalogFilterMenuList
                  options={presentationOptions}
                  value={filters.presentationKey}
                  allLabel="Todas las presentaciones"
                  onSelect={(value) =>
                    setFilters((current) => ({
                      ...current,
                      presentationKey: value,
                    }))
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
                  setFilters((current) => ({
                    ...current,
                    priceMin,
                    priceMax,
                  }))
                }
              />
            ) : null}
          </div>

          {filtersActive ? (
            <div className="mt-3 flex flex-wrap items-center gap-2">
              {filters.productTypeId !== null && selectedTypeLabel ? (
                <ActiveFilterChip
                  label="Tipo"
                  value={selectedTypeLabel}
                  onClear={() =>
                    setFilters((current) => withProductType(current, null))
                  }
                />
              ) : null}
              {filters.productVariantId !== null && selectedVariantLabel ? (
                <ActiveFilterChip
                  label="Versión"
                  value={selectedVariantLabel}
                  onClear={() =>
                    setFilters((current) => withProductVariant(current, null))
                  }
                />
              ) : null}
              {filters.presentationKey !== null && selectedPresentationLabel ? (
                <ActiveFilterChip
                  label="Presentación"
                  value={selectedPresentationLabel}
                  onClear={() =>
                    setFilters((current) => ({
                      ...current,
                      presentationKey: null,
                    }))
                  }
                />
              ) : null}
              {priceActive ? (
                <ActiveFilterChip
                  label="Precio"
                  value={formatPriceFilterLabel(
                    filters.priceMin,
                    filters.priceMax,
                  )}
                  onClear={() =>
                    setFilters((current) => ({
                      ...current,
                      priceMin: null,
                      priceMax: null,
                    }))
                  }
                />
              ) : null}
              <button
                type="button"
                onClick={() => setFilters(EMPTY_PRODUCT_CATALOG_FILTERS)}
                className="min-h-8 px-2 text-sm font-semibold text-sf-primary transition-colors hover:underline"
              >
                Quitar filtros
              </button>
            </div>
          ) : null}
        </>
      ) : null}

      <div className="mt-4 md:mt-5">
        {productsQuery.isPending ? (
          <ProductGridSkeleton />
        ) : productsQuery.isError ? (
          <CatalogQueryError error={productsQuery.error} title="No se pudieron cargar los productos" />
        ) : visibleProducts.length > 0 ? (
          <ProductGrid products={visibleProducts} />
        ) : filtersActive ? (
          <EmptyState
            title="No encontramos productos con estos filtros."
            description="Prueba quitando algún filtro o explorando otra categoría."
            align="center"
            className="py-14"
            icon={<SearchIcon className="size-6" />}
            action={
              <button
                type="button"
                className="text-sm font-semibold text-sf-primary hover:underline"
                onClick={() => setFilters(EMPTY_PRODUCT_CATALOG_FILTERS)}
              >
                Quitar filtros
              </button>
            }
          />
        ) : (
          <EmptyState
            title={emptyTitle}
            description={emptyDescription}
            align="center"
            className="py-14"
            icon={<SearchIcon className="size-6" />}
          />
        )}
      </div>
    </Container>
  );
}

function ActiveFilterChip({
  label,
  value,
  onClear,
}: {
  label: string;
  value: string;
  onClear: () => void;
}) {
  return (
    <span className="inline-flex min-h-8 items-center gap-1.5 rounded-full bg-sf-primary-soft py-1 pl-3 pr-1.5 text-xs font-semibold text-sf-primary">
      <span className="text-sf-primary/80">{label}:</span>
      <span>{value}</span>
      <button
        type="button"
        aria-label={`Quitar filtro ${label}`}
        onClick={onClear}
        className="flex size-5 items-center justify-center rounded-full transition-colors duration-150 hover:bg-sf-primary/20"
      >
        <CloseIcon className="h-3 w-3" aria-hidden="true" />
      </button>
    </span>
  );
}
