"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { lookupProductByBarcode } from "@/features/admin/api";
import { AdminProductForm } from "@/features/admin/components/admin-product-form";
import {
  useAdminCategoriesQuery,
  useCreateAdminProductMutation,
} from "@/features/admin/hooks";
import {
  applyBarcodeSuggestion,
  createAdminProductRequestFromValues,
  emptyAdminProductFormValues,
  validateCreateAdminProduct,
} from "@/features/admin/payloads";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { Skeleton } from "@/shared/ui/skeleton";
import { TextField } from "@/shared/ui/text-field";

type LookupState = "idle" | "loading" | "found" | "not_found" | "error";

export function AdminCreateProductPageContent() {
  const router = useRouter();
  const categoriesQuery = useAdminCategoriesQuery();
  const createMutation = useCreateAdminProductMutation();
  const [values, setValues] = useState(emptyAdminProductFormValues);
  const [lookupBarcode, setLookupBarcode] = useState("");
  const [lookupState, setLookupState] = useState<LookupState>("idle");
  const [lookupMessage, setLookupMessage] = useState<string | null>(null);

  const error = createMutation.isError
    ? isApiError(createMutation.error)
      ? messageForApiProblem(createMutation.error.problem)
      : "No se pudo crear el producto."
    : null;

  async function handleBarcodeLookup() {
    const barcode = lookupBarcode.trim() || values.barcode.trim();
    if (!barcode) {
      setLookupState("error");
      setLookupMessage("Escribe un código de barras para buscar.");
      return;
    }
    setLookupState("loading");
    setLookupMessage(null);
    try {
      const suggestion = await lookupProductByBarcode(barcode);
      setValues((current) => applyBarcodeSuggestion(current, suggestion));
      setLookupBarcode(suggestion.barcode);
      setLookupState("found");
      setLookupMessage(
        "Datos cargados desde Open Food Facts. Revisa precio, stock y categoría antes de guardar.",
      );
    } catch (caught) {
      if (isApiError(caught) && caught.problem.code === "BARCODE_LOOKUP_NOT_FOUND") {
        setLookupState("not_found");
        setLookupMessage(messageForApiProblem(caught.problem));
        return;
      }
      setLookupState("error");
      setLookupMessage(
        isApiError(caught)
          ? messageForApiProblem(caught.problem)
          : "No se pudo consultar Open Food Facts.",
      );
    }
  }

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
        Puedes buscar un código de barras en Open Food Facts para prellenar
        algunos campos. El precio, el stock y la categoría los defines tú. El
        stock inicial se define aquí; después también puedes ajustarlo desde el
        detalle del producto.
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
        <div className="mt-8 grid gap-6">
          <Card>
            <h2 className="text-lg font-semibold text-sf-ink">
              Open Food Facts
            </h2>
            <p className="mt-1 text-sm text-sf-muted">
              Solo prellena nombre, marca, descripción e imagen. No guarda el
              producto.
            </p>
            <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
              <div className="min-w-0 flex-1">
                <TextField
                  id="off-barcode"
                  label="Código de barras"
                  value={lookupBarcode}
                  autoComplete="off"
                  onChange={(event) => {
                    setLookupBarcode(event.target.value);
                    if (lookupState !== "idle") {
                      setLookupState("idle");
                      setLookupMessage(null);
                    }
                  }}
                />
              </div>
              <Button
                type="button"
                variant="secondary"
                disabled={lookupState === "loading"}
                onClick={() => void handleBarcodeLookup()}
              >
                {lookupState === "loading"
                  ? "Buscando…"
                  : "Buscar en Open Food Facts"}
              </Button>
            </div>
            {lookupMessage ? (
              <div className="mt-4">
                <Alert
                  tone={
                    lookupState === "found"
                      ? "success"
                      : lookupState === "not_found"
                        ? "warning"
                        : lookupState === "error"
                          ? "error"
                          : "info"
                  }
                  title={
                    lookupState === "found"
                      ? "Producto encontrado"
                      : lookupState === "not_found"
                        ? "No encontrado"
                        : lookupState === "error"
                          ? "No se pudo buscar"
                          : "Open Food Facts"
                  }
                >
                  {lookupMessage}
                </Alert>
              </div>
            ) : null}
          </Card>

          <Card>
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
                createMutation.mutate(
                  createAdminProductRequestFromValues(values),
                  {
                    onSuccess: (product) => {
                      router.push(`/admin/products/${product.id}?created=1`);
                    },
                  },
                );
              }}
            />
          </Card>
        </div>
      ) : null}
    </Container>
  );
}
