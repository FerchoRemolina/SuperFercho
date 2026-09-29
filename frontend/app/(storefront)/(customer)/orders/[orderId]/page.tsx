import { OrderResultContent } from "@/features/orders/components/order-result-content";

export default async function OrderDetailPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const { orderId } = await params;
  return <OrderResultContent orderId={orderId} />;
}
