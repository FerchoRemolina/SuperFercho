import { AdminOrderDetailPageContent } from "@/features/admin/components/admin-order-detail-page";

export default async function AdminOrderDetailPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const { orderId } = await params;

  return <AdminOrderDetailPageContent key={orderId} orderId={orderId} />;
}
