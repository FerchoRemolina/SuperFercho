"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useAdminProductsQuery } from "@/features/admin/hooks";
import {
  adminInventoryHref,
  partitionAdminStockAttention,
} from "@/features/admin/presentation";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { CheckCircleIcon, WarningIcon } from "@/shared/ui/icons";
import { Skeleton } from "@/shared/ui/skeleton";

/** Discrete hub signal: real ACTIVE stock attention counts only. */
export function AdminInventoryAttentionSummary() {
  const productsQuery = useAdminProductsQuery({ status: "ACTIVE" });
  const buckets = useMemo(
    () => partitionAdminStockAttention(productsQuery.data ?? []),
    [productsQuery.data],
  );
  const total = buckets.lowStock.length + buckets.outOfStock.length;

  if (productsQuery.isPending) {
    return (
      <Card className="rounded-2xl p-5 shadow-[0_1px_3px_rgba(23,33,27,0.05)]">
        <Skeleton className="h-5 w-48" />
        <Skeleton className="mt-3 h-4 w-72" />
      </Card>
    );
  }

  if (productsQuery.isError) {
    return null;
  }

  if (total === 0) {
    return (
      <Card className="rounded-2xl border-sf-border p-5 shadow-[0_1px_3px_rgba(23,33,27,0.05)]">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
            <CheckCircleIcon className="h-5 w-5" />
          </span>
          <div>
            <h2 className="text-base font-bold text-sf-ink">
              Inventario al día
            </h2>
            <p className="mt-1 text-sm text-sf-muted">
              No hay productos agotados ni próximos a agotarse.
            </p>
          </div>
        </div>
      </Card>
    );
  }

  return (
    <Card className="rounded-2xl border-amber-200/80 bg-amber-50/40 p-5 shadow-[0_1px_3px_rgba(23,33,27,0.05)]">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-800">
            <WarningIcon className="h-5 w-5" />
          </span>
          <div>
            <h2 className="text-base font-bold text-sf-ink">
              Inventario requiere atención
            </h2>
            <p className="mt-1 text-sm text-sf-muted">
              {buckets.outOfStock.length} agotados · {buckets.lowStock.length}{" "}
              próximos a agotarse
            </p>
          </div>
        </div>
        <Link
          href={adminInventoryHref()}
          className={buttonClassName("secondary")}
        >
          Ir a control de inventario
        </Link>
      </div>
    </Card>
  );
}
