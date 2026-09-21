import { ProductDetailContent } from "@/features/catalog/components/product-detail-content";

export default async function ProductPage({
  params,
}: {
  params: Promise<{ productId: string }>;
}) {
  const { productId } = await params;
  return <ProductDetailContent productId={productId} />;
}
