export type FavoriteToggleVariant = "icon" | "action";

export function canShowFavoriteToggle(role: string | null | undefined): boolean {
  return role !== "ADMIN";
}

export function favoriteToggleLabel(favorited: boolean): string {
  return favorited ? "Quitar de favoritos" : "Añadir a favoritos";
}

export function favoriteHeartFill(favorited: boolean): "currentColor" | "none" {
  return favorited ? "currentColor" : "none";
}

export function favoriteToggleShowsHeart(variant: FavoriteToggleVariant): boolean {
  return variant === "icon";
}
