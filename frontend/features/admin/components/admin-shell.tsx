"use client";

import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { AdminHeader } from "@/features/admin/components/admin-header";
import { AdminSidebar } from "@/features/admin/components/admin-sidebar";
import { useAdminProductsQuery } from "@/features/admin/hooks";
import {
  countAdminInventoryAttention,
  partitionAdminStockAttention,
} from "@/features/admin/presentation";

export function AdminShell({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const productsQuery = useAdminProductsQuery({ status: "ACTIVE" });
  const inventoryAttentionCount = useMemo(() => {
    if (!productsQuery.data) {
      return 0;
    }
    return countAdminInventoryAttention(
      partitionAdminStockAttention(productsQuery.data),
    );
  }, [productsQuery.data]);

  return (
    <div className="min-h-screen bg-sf-bg">
      <AdminSidebar
        inventoryAttentionCount={inventoryAttentionCount}
        mobileOpen={mobileOpen}
        onCloseMobile={() => setMobileOpen(false)}
      />
        <div className="lg:pl-[16rem]">
        <AdminHeader onOpenMobileNav={() => setMobileOpen(true)} />
        <div className="min-h-[calc(100vh-4rem)]">{children}</div>
      </div>
    </div>
  );
}
