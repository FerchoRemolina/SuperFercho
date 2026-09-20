import { PlaceholderScreen } from "@/shared/ui/placeholder-screen";

export default function AdminPaymentPage() {
  return (
    <PlaceholderScreen
      title="Pago (admin)"
      description="La consulta de pago usará GET /api/v1/payments/{paymentId}. Solo ADMIN."
    />
  );
}
