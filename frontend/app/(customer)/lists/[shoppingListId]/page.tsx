import { ListDetailPageContent } from "@/features/lists/components/list-detail-page-content";

export default async function ListDetailPage({
  params,
}: {
  params: Promise<{ shoppingListId: string }>;
}) {
  const { shoppingListId } = await params;
  return <ListDetailPageContent shoppingListId={shoppingListId} />;
}
