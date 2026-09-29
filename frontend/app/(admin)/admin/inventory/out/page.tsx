import { AdminStockAttentionPanel } from "@/features/admin/components/admin-stock-attention-panel";

export default function AdminInventoryOutPage() {
  return (
    <div className="px-4 py-6 md:px-8 md:py-8">
      <header className="mb-6 max-w-3xl">
        <h1 className="text-2xl font-bold tracking-tight text-sf-ink md:text-3xl">
          Agotados
        </h1>
        <p className="mt-2 text-sm text-sf-muted md:text-base">
          Productos activos con stock en cero.
        </p>
      </header>
      <AdminStockAttentionPanel
        focus="out"
        showHeading={false}
        className="mt-0"
      />
    </div>
  );
}
