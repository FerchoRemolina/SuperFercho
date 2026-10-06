"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { AdminProductForm } from "@/features/admin/components/admin-product-form";
import { ProductPricePanel } from "@/features/admin/components/product-price-panel";
import { ProductStockPanel } from "@/features/admin/components/product-stock-panel";
import { ProductStatusActions } from "@/features/admin/components/product-status-actions";
import { ProductStatusBadge } from "@/features/admin/components/product-status-badge";
import {
  useAdminCategoriesQuery,
  useAdminProductQuery,
  useUpdateAdminProductMutation,
} from "@/features/admin/hooks";
import {
  adminProductFormValuesFromProduct,
  type AdminProductFormValues,
  updateAdminProductRequestFromValues,
  validateUpdateAdminProduct,
} from "@/features/admin/payloads";
import { formatAdminInstant, adminProductsListHrefFromSearchParams } from "@/features/admin/presentation";
import { ProductImage } from "@/features/catalog/components/product-image";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { BackLink } from "@/shared/ui/back-link";
import { Card } from "@/shared/ui/card";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminProductDetailPageContent({
  productId,
}: {
  productId: string;
}) {
  const searchParams = useSearchParams();
  const created = searchParams.get("created") === "1";
  const listHref = adminProductsListHrefFromSearchParams({
    text: searchParams.get("text"),
    categoryId: searchParams.get("categoryId"),
    status: searchParams.get("status"),
  });
  const productQuery = useAdminProductQuery(productId);
  const categoriesQuery = useAdminCategoriesQuery();
  const updateMutation = useUpdateAdminProductMutation();
  const [draftValues, setDraftValues] = useState<AdminProductFormValues | null>(
    null,
  );
  const [saved, setSaved] = useState(false);

  const product = productQuery.data;
  const values =
    draftValues ??
    (product ? adminProductFormValuesFromProduct(product) : null);

  const categoryName = useMemo(() => {
    if (!product) {
      return null;
    }
    return (
      categoriesQuery.data?.find((category) => category.id === product.categoryId)
        ?.name ?? null
    );
  }, [categoriesQuery.data, product]);

  const updateError = updateMutation.isError
    ? isApiError(updateMutation.error)
      ? messageForApiProblem(updateMutation.error.problem)
      : "No se pudo guardar el producto."
    : null;

  if (productQuery.isPending) {
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-96 w-full" />
      </main>
    );
  }

  if (productQuery.isError) {
    const notFound =
      isApiError(productQuery.error) &&
      productQuery.error.problem.code === "PRODUCT_NOT_FOUND";
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        {notFound ? (
          <EmptyState
            title="No encontramos este producto"
            description="Puede que el identificador no exista en el catálogo."
            action={
              <Link href={listHref} className={buttonClassName("secondary")}>
                Volver al listado
              </Link>
            }
          />
        ) : (
          <Alert tone="error" title="No se pudo cargar el producto">
            {messageForApiProblem(
              isApiError(productQuery.error)
                ? productQuery.error.problem
                : { status: 500 },
            )}
          </Alert>
        )}
      </main>
    );
  }

  if (!product || !values) {
    return (
      <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-96 w-full" />
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <BackLink href={listHref} className="mb-4">Volver al listado</BackLink>

      {created ? (
        <div className="mb-6">
          <Alert tone="success" title="Producto creado">
            Ya puedes revisar la ficha y cambiar el precio si lo necesitas.
          </Alert>
        </div>
      ) : null}

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
            Inventario · Productos
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
            {product.name}
          </h1>
          <p className="mt-2 text-sm text-sf-muted">
            {[product.brand ?? "Sin marca", categoryName]
              .filter((part): part is string => Boolean(part))
              .join(" · ")}
          </p>
          <p className="mt-1 text-xs text-sf-muted">
            Creado {formatAdminInstant(product.createdAt)} · Actualizado{" "}
            {formatAdminInstant(product.updatedAt)}
            {product.barcode ? ` · Código: ${product.barcode}` : ""}
          </p>
        </div>
        <ProductStatusBadge status={product.status} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[16rem_1fr]">
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          className="max-w-xs"
        />
        <div className="grid content-start gap-5">
          <ProductPricePanel product={product} />

          <ProductStockPanel product={product} />

          <Card className="grid gap-4">
            <h2 className="text-lg font-semibold text-sf-ink">Ficha</h2>
            {saved ? (
              <p className="text-sm font-semibold text-sf-success">
                Cambios guardados correctamente.
              </p>
            ) : null}
            {categoriesQuery.isSuccess ? (
              <AdminProductForm
                mode="edit"
                values={values}
                categories={categoriesQuery.data}
                pending={updateMutation.isPending}
                error={updateError}
                onChange={setDraftValues}
                validate={validateUpdateAdminProduct}
                onCancel={() => setDraftValues(null)}
                onSubmit={() => {
                  setSaved(false);
                  updateMutation.mutate(
                    {
                      productId: product.id,
                      body: updateAdminProductRequestFromValues(values),
                    },
                    {
                      onSuccess: () => {
                        setDraftValues(null);
                        setSaved(true);
                      },
                    },
                  );
                }}
              />
            ) : (
              <Skeleton className="h-64 w-full" />
            )}
          </Card>

          <Card className="grid gap-3">
            <h2 className="text-lg font-semibold text-sf-ink">Estado</h2>
            <ProductStatusActions product={product} />
          </Card>
        </div>
      </div>
    </main>
  );
}
