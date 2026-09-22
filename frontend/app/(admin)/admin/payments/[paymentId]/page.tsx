import { AdminPaymentDetailPage } from "@/features/admin/components/admin-payment-detail-page";

export default async function AdminPaymentPage({
  params,
}: {
  params: Promise<{ paymentId: string }>;
}) {
  const { paymentId } = await params;

  return (
    <AdminPaymentDetailPage key={paymentId} paymentId={paymentId} />
  );
}
