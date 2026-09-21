"use client";

import { CatalogBrowse } from "@/features/catalog/components/catalog-browse";

export function CatalogPageContent() {
  return (
    <CatalogBrowse
      title="Catálogo"
      description="Productos disponibles en SuperFercho."
      emptyTitle="No encontramos productos."
      emptyDescription="Cuando haya productos en el súper, los verás aquí."
    />
  );
}
