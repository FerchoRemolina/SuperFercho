"use client";

import { useRouter } from "next/navigation";
import { isProductInFavorites } from "@/features/favorites/api";
import {
  useAddFavoriteMutation,
  useFavoritesQuery,
  useRemoveFavoriteMutation,
} from "@/features/favorites/hooks";
import { loginPathWithNext } from "@/shared/auth/safe-next-path";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { useSession } from "@/shared/session/session-provider";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { HeartIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

export function FavoriteToggle({
  productId,
  className,
}: {
  productId: string;
  className?: string;
}) {
  const { session } = useSession();
  const router = useRouter();
  const favoritesQuery = useFavoritesQuery();
  const addMutation = useAddFavoriteMutation();
  const removeMutation = useRemoveFavoriteMutation();
  const favorited = isProductInFavorites(favoritesQuery.data?.items, productId);
  const mutating = addMutation.isPending || removeMutation.isPending;

  if (session?.role === "ADMIN") {
    return null;
  }

  async function onToggle() {
    if (!session) {
      const currentPath = `${window.location.pathname}${window.location.search}`;
      router.push(loginPathWithNext(currentPath));
      return;
    }
    if (mutating) {
      return;
    }
    try {
      if (favorited) {
        await removeMutation.mutateAsync(productId);
      } else {
        await addMutation.mutateAsync(productId);
      }
    } catch {
      // Error is rendered from mutation state.
    }
  }

  const error = addMutation.error ?? removeMutation.error;
  const errorMessage = error
    ? isApiError(error)
      ? messageForApiProblem(error.problem)
      : "No se pudo actualizar el favorito."
    : null;

  const label = favorited ? "Quitar de favoritos" : "Añadir a favoritos";

  return (
    <div className={cx("grid gap-2", className)}>
      {errorMessage ? (
        <Alert tone="error" title="No se pudo actualizar">
          {errorMessage}
        </Alert>
      ) : null}
      <Button
        type="button"
        variant="secondary"
        onClick={onToggle}
        disabled={mutating}
        aria-pressed={favorited}
        aria-label={label}
        title={label}
        className="w-full gap-2"
      >
        <HeartIcon fill={favorited ? "currentColor" : "none"} />
        {favorited ? "En favoritos" : "Favorito"}
      </Button>
    </div>
  );
}
