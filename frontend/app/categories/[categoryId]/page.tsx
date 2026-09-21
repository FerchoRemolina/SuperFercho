import { CategoryPageContent } from "@/features/catalog/components/category-page-content";

export default async function CategoryPage({
  params,
}: {
  params: Promise<{ categoryId: string }>;
}) {
  const { categoryId } = await params;
  return <CategoryPageContent categoryId={categoryId} />;
}
