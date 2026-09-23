export function formatListInstant(iso: string): string {
  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Bogota",
  }).format(new Date(iso));
}

export function shoppingListItemsLabel(itemCount: number): string {
  if (itemCount === 1) {
    return "1 producto";
  }
  return `${itemCount} productos`;
}

export function shoppingListHref(shoppingListId: string): string {
  return `/lists/${encodeURIComponent(shoppingListId)}`;
}
