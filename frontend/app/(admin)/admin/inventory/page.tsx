import { AdminStockAttentionPanel } from "@/features/admin/components/admin-stock-attention-panel";

export default function AdminInventoryPage() {
  return (
    <div className="px-4 py-6 md:px-8 md:py-8">
      <header className="mb-6 max-w-3xl">
        <h1 className="text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
          Control de inventario
        </h1>
        <p className="mt-2 text-sm text-sf-muted md:text-base">
          Productos activos agotados o próximos a agotarse (1–5 unidades).
        </p>
      </header>
      <AdminStockAttentionPanel className="mt-0" />
    </div>
  );
}
