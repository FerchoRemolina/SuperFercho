import { AdminStockFocusView } from "@/features/admin/components/admin-stock-focus-view";

export default function AdminInventoryLowPage() {
  return (
    <main className="mx-auto w-full max-w-[90rem] px-4 py-6 md:px-8 md:py-8">
      <p className="text-xs font-semibold uppercase tracking-[0.08em] text-sf-muted">
        Inventario
      </p>
      <h1 className="mt-1 text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
        Próximos a agotarse
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-sf-muted md:text-base">
        Productos con existencias bajas que requieren atención.
      </p>
      <AdminStockFocusView focus="low" />
    </main>
  );
}
