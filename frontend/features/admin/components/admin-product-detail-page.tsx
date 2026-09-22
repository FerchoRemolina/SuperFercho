"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AdminProductForm } from "@/features/admin/components/admin-product-form";
import { ProductPricePanel } from "@/features/admin/components/product-price-panel";
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
import { formatAdminInstant } from "@/features/admin/presentation";
import { productStockLabel } from "@/features/catalog/quantity";
import { ProductImage } from "@/features/catalog/components/product-image";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminProductDetailPageContent({
  productId,
}: {
  productId: string;
}) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const created = searchParams.get("created") === "1";
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
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-96 w-full" />
      </Container>
    );
  }

  if (productQuery.isError) {
    const notFound =
      isApiError(productQuery.error) &&
      productQuery.error.problem.code === "PRODUCT_NOT_FOUND";
    return (
      <Container as="main" className="py-10 md:py-16">
        {notFound ? (
          <EmptyState
            title="No encontramos este producto"
            description="Puede que el identificador no exista en el catálogo."
            action={
              <Link href="/admin/products" className={buttonClassName("secondary")}>
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
      </Container>
    );
  }

  if (!product || !values) {
    return (
      <Container as="main" className="py-10 md:py-16">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-8 h-96 w-full" />
      </Container>
    );
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href="/admin/products"
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>

      {created ? (
        <div className="mb-6">
          <Alert tone="success" title="Producto creado">
            Ya puedes revisar la ficha y cambiar el precio si lo necesitas.
          </Alert>
        </div>
      ) : null}

      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            {product.name}
          </h1>
          <p className="mt-2 text-base text-sf-muted">
            {product.brand ?? "Sin marca"}
            {categoryName ? ` · ${categoryName}` : null}
          </p>
        </div>
        <ProductStatusBadge status={product.status} />
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,20rem)_1fr]">
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          className="max-w-xs"
        />
        <div className="grid gap-6">
          <Card className="grid gap-3 text-sm">
            <h2 className="text-lg font-semibold text-sf-ink">
              Información actual
            </h2>
            <dl className="grid gap-2 sm:grid-cols-2">
              <div>
                <dt className="text-sf-muted">Precio</dt>
                <dd className="font-semibold text-sf-ink">
                  {formatMoney(product.price)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Stock</dt>
                <dd className="font-semibold text-sf-ink">
                  {product.stock} · {productStockLabel(product.stock)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Código de barras</dt>
                <dd className="text-sf-ink">{product.barcode ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-sf-muted">Creado</dt>
                <dd className="text-sf-ink">
                  {formatAdminInstant(product.createdAt)}
                </dd>
              </div>
              <div>
                <dt className="text-sf-muted">Actualizado</dt>
                <dd className="text-sf-ink">
                  {formatAdminInstant(product.updatedAt)}
                </dd>
              </div>
            </dl>
          </Card>

          <ProductPricePanel product={product} />

          <Card className="grid gap-4">
            <h2 className="text-xl font-semibold text-sf-ink">Ficha</h2>
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
            <h2 className="text-xl font-semibold text-sf-ink">Estado</h2>
            <ProductStatusActions product={product} />
          </Card>
        </div>
      </div>

      <div className="mt-8">
        <Button
          type="button"
          variant="secondary"
          onClick={() => router.push("/admin/products")}
        >
          Volver al listado
        </Button>
      </div>
    </Container>
  );
}
