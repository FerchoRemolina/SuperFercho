"use client";

import { useMemo, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ProductStatusActions } from "@/features/admin/components/product-status-actions";
import { ProductStatusBadge } from "@/features/admin/components/product-status-badge";
import {
  useAdminCategoriesQuery,
  useAdminProductsQuery,
} from "@/features/admin/hooks";
import {
  adminProductsHref,
  listQueryFromSearchParams,
  shouldSearchAdminProducts,
} from "@/features/admin/presentation";
import { productStockLabel } from "@/features/catalog/quantity";
import type { ProductStatus } from "@/features/catalog/api";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { formatMoney } from "@/shared/money/money";
import { Alert } from "@/shared/ui/alert";
import { Button, buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { EmptyState } from "@/shared/ui/empty-state";
import { SelectField } from "@/shared/ui/select-field";
import { Skeleton } from "@/shared/ui/skeleton";

export function AdminProductsPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const text = searchParams.get("text") ?? "";
  const searching = shouldSearchAdminProducts(text);
  const listQuery = listQueryFromSearchParams({
    categoryId: searchParams.get("categoryId") ?? undefined,
    status: searchParams.get("status") ?? undefined,
  });
  const productsQuery = useAdminProductsQuery({
    text,
    categoryId: listQuery.categoryId,
    status: listQuery.status,
  });
  const categoriesQuery = useAdminCategoriesQuery();
  const categoriesById = useMemo(() => {
    const map = new Map<string, string>();
    for (const category of categoriesQuery.data ?? []) {
      map.set(category.id, category.name);
    }
    return map;
  }, [categoriesQuery.data]);

  function replaceFilters(next: {
    text?: string;
    categoryId?: string;
    status?: ProductStatus | "";
  }) {
    router.replace(
      adminProductsHref({
        text: next.text ?? text,
        categoryId: next.categoryId ?? listQuery.categoryId,
        status: next.status === undefined ? listQuery.status : next.status,
      }),
    );
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submitted = String(
      new FormData(event.currentTarget).get("text") ?? "",
    ).trim();
    router.replace(
      adminProductsHref({
        text: submitted,
        categoryId: listQuery.categoryId,
        status: listQuery.status,
      }),
    );
  }

  return (
    <Container as="main" className="py-10 md:py-16">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
            Productos
          </h1>
          <p className="mt-2 max-w-2xl text-base text-sf-muted">
            El stock se define al crear el producto. Después de crearlo solo se
            consulta; no hay ajuste de inventario en esta fase.
          </p>
        </div>
        <Link href="/admin/products/new" className={buttonClassName("primary")}>
          Crear producto
        </Link>
      </div>

      <form
        className="mt-8 grid gap-3 md:grid-cols-[1fr_auto]"
        role="search"
        onSubmit={onSearch}
      >
        <label htmlFor="admin-product-search" className="sr-only">
          Buscar productos
        </label>
        <input
          id="admin-product-search"
          type="search"
          name="text"
          defaultValue={text}
          placeholder="Buscar por nombre, marca o código"
          className="min-h-11 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink"
        />
        <Button type="submit">Buscar</Button>
      </form>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <SelectField
          id="admin-product-category"
          label="Categoría"
          value={searching ? "" : (listQuery.categoryId ?? "")}
          disabled={searching}
          onChange={(event) =>
            replaceFilters({ categoryId: event.target.value || undefined })
          }
        >
          <option value="">Todas</option>
          {(categoriesQuery.data ?? []).map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </SelectField>
        <SelectField
          id="admin-product-status"
          label="Estado"
          value={searching ? "" : (listQuery.status ?? "")}
          disabled={searching}
          onChange={(event) =>
            replaceFilters({
              status: (event.target.value || "") as ProductStatus | "",
            })
          }
        >
          <option value="">Todos</option>
          <option value="ACTIVE">Activo</option>
          <option value="INACTIVE">Inactivo</option>
        </SelectField>
      </div>
      {searching ? (
        <p className="mt-2 text-sm text-sf-muted">
          La búsqueda usa el catálogo del servidor. Quita el texto para filtrar
          por categoría o estado.
        </p>
      ) : null}

      {productsQuery.isPending ? (
        <div className="mt-8 grid gap-3" aria-hidden="true">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : null}

      {productsQuery.isError ? (
        <div className="mt-8">
          <Alert tone="error" title="No se pudo cargar el catálogo">
            {isApiError(productsQuery.error)
              ? messageForApiProblem(productsQuery.error.problem)
              : "No se pudo completar la solicitud."}
          </Alert>
        </div>
      ) : null}

      {productsQuery.isSuccess && productsQuery.data.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            title="No hay productos para mostrar"
            description={
              searching
                ? "Prueba con otro término o limpia la búsqueda."
                : "Crea el primer producto para el catálogo."
            }
            action={
              searching ? (
                <Link
                  href="/admin/products"
                  className={buttonClassName("secondary")}
                >
                  Ver todos
                </Link>
              ) : (
                <Link
                  href="/admin/products/new"
                  className={buttonClassName("primary")}
                >
                  Crear producto
                </Link>
              )
            }
          />
        </div>
      ) : null}

      {productsQuery.isSuccess && productsQuery.data.length > 0 ? (
        <>
          <ul className="mt-8 grid gap-4 md:hidden">
            {productsQuery.data.map((product) => (
              <li key={product.id}>
                <Card className="grid gap-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-lg font-semibold text-sf-ink">
                        {product.name}
                      </p>
                      <p className="text-sm text-sf-muted">
                        {product.brand ?? "Sin marca"} ·{" "}
                        {categoriesById.get(product.categoryId) ?? "Categoría"}
                      </p>
                    </div>
                    <ProductStatusBadge status={product.status} />
                  </div>
                  <p className="text-base font-semibold text-sf-ink">
                    {formatMoney(product.price)}
                  </p>
                  <p className="text-sm text-sf-muted">
                    Stock: {product.stock} · {productStockLabel(product.stock)}
                  </p>
                  <div className="grid gap-2">
                    <Link
                      href={`/admin/products/${product.id}`}
                      className={buttonClassName("secondary")}
                    >
                      Ver y editar
                    </Link>
                    <ProductStatusActions product={product} compact />
                  </div>
                </Card>
              </li>
            ))}
          </ul>

          <div className="mt-8 hidden overflow-x-auto md:block">
            <table className="w-full min-w-[52rem] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-sf-border text-sf-muted">
                  <th className="px-3 py-3 font-semibold">Nombre</th>
                  <th className="px-3 py-3 font-semibold">Marca</th>
                  <th className="px-3 py-3 font-semibold">Categoría</th>
                  <th className="px-3 py-3 font-semibold">Precio</th>
                  <th className="px-3 py-3 font-semibold">Stock</th>
                  <th className="px-3 py-3 font-semibold">Estado</th>
                  <th className="px-3 py-3 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {productsQuery.data.map((product) => (
                  <tr
                    key={product.id}
                    className="border-b border-sf-border align-top"
                  >
                    <td className="px-3 py-4 font-semibold text-sf-ink">
                      {product.name}
                    </td>
                    <td className="px-3 py-4 text-sf-muted">
                      {product.brand ?? "—"}
                    </td>
                    <td className="px-3 py-4 text-sf-muted">
                      {categoriesById.get(product.categoryId) ?? "—"}
                    </td>
                    <td className="px-3 py-4 text-sf-ink">
                      {formatMoney(product.price)}
                    </td>
                    <td className="px-3 py-4 text-sf-muted">
                      <span className="block font-semibold text-sf-ink">
                        {product.stock}
                      </span>
                      {productStockLabel(product.stock)}
                    </td>
                    <td className="px-3 py-4">
                      <ProductStatusBadge status={product.status} />
                    </td>
                    <td className="px-3 py-4">
                      <div className="grid max-w-56 gap-2">
                        <Link
                          href={`/admin/products/${product.id}`}
                          className={buttonClassName("secondary")}
                        >
                          Ver y editar
                        </Link>
                        <ProductStatusActions product={product} compact />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </Container>
  );
}
