import { AdminStockAttentionPanel } from "@/features/admin/components/admin-stock-attention-panel";

export default function AdminInventoryLowPage() {
  return (
    <div className="px-4 py-6 md:px-8 md:py-8">
      <header className="mb-6 max-w-3xl">
        <h1 className="text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
          Próximos a agotarse
        </h1>
        <p className="mt-2 text-sm text-sf-muted md:text-base">
          Productos activos con stock entre 1 y 5 unidades.
        </p>
      </header>
      <AdminStockAttentionPanel
        focus="low"
        showHeading={false}
        className="mt-0"
      />
    </div>
  );
}
