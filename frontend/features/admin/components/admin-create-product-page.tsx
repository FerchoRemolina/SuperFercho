"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AdminProductForm } from "@/features/admin/components/admin-product-form";
import {
  useAdminCategoriesQuery,
  useCreateAdminProductMutation,
} from "@/features/admin/hooks";
import {
  createAdminProductRequestFromValues,
  emptyAdminProductFormValues,
  validateCreateAdminProduct,
} from "@/features/admin/payloads";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminCreateProductPageContent() {
  const router = useRouter();
  const categoriesQuery = useAdminCategoriesQuery();
  const createMutation = useCreateAdminProductMutation();
  const [values, setValues] = useState(emptyAdminProductFormValues);

  const error = createMutation.isError
    ? isApiError(createMutation.error)
      ? messageForApiProblem(createMutation.error.problem)
      : "No se pudo crear el producto."
    : null;

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href="/admin/products"
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Nuevo producto
      </h1>
      <p className="mt-2 max-w-2xl text-base text-sf-muted">
        El stock inicial se define aquí. Después también puedes ajustarlo desde
        el detalle del producto.
      </p>

      {categoriesQuery.isPending ? (
        <Skeleton className="mt-8 h-96 w-full" />
      ) : null}

      {categoriesQuery.isError ? (
        <div className="mt-8">
          <Alert tone="error" title="No se pudieron cargar las categorías">
            {isApiError(categoriesQuery.error)
              ? messageForApiProblem(categoriesQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
        </div>
      ) : null}

      {categoriesQuery.isSuccess ? (
        <Card className="mt-8">
          <AdminProductForm
            mode="create"
            values={values}
            categories={categoriesQuery.data}
            pending={createMutation.isPending}
            error={error}
            onChange={setValues}
            validate={validateCreateAdminProduct}
            onCancel={() => router.push("/admin/products")}
            onSubmit={() => {
              createMutation.mutate(createAdminProductRequestFromValues(values), {
                onSuccess: (product) => {
                  router.push(
                    `/admin/products/${product.id}?created=1`,
                  );
                },
              });
            }}
          />
        </Card>
      ) : null}
    </Container>
  );
}
