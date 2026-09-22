"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AdminCategoryForm } from "@/features/admin/components/admin-category-form";
import { useCreateAdminCategoryMutation } from "@/features/admin/hooks";
import {
  createAdminCategoryRequestFromValues,
  emptyAdminCategoryFormValues,
  validateAdminCategory,
} from "@/features/admin/payloads";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";

export function AdminCreateCategoryPageContent() {
  const router = useRouter();
  const createMutation = useCreateAdminCategoryMutation();
  const [values, setValues] = useState(emptyAdminCategoryFormValues);

  const error = createMutation.isError
    ? isApiError(createMutation.error)
      ? messageForApiProblem(createMutation.error.problem)
      : "No se pudo crear la categoría."
    : null;

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href="/admin/categories"
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Nueva categoría
      </h1>
      <p className="mt-2 max-w-2xl text-base text-sf-muted">
        La categoría se crea activa. Después puedes desactivarla desde el
        detalle.
      </p>

      <Card className="mt-8">
        <AdminCategoryForm
          mode="create"
          values={values}
          pending={createMutation.isPending}
          error={error}
          onChange={setValues}
          validate={validateAdminCategory}
          onCancel={() => router.push("/admin/categories")}
          onSubmit={() => {
            createMutation.mutate(createAdminCategoryRequestFromValues(values), {
              onSuccess: (category) => {
                router.push(`/admin/categories/${category.id}?created=1`);
              },
            });
          }}
        />
      </Card>
    </Container>
  );
}
