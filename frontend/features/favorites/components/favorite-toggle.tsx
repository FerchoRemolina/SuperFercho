"use client";

import { useState, type MouseEvent } from "react";
import { useRouter } from "next/navigation";
import { isProductInFavorites } from "@/features/favorites/api";
import {
  useAddFavoriteMutation,
  useFavoritesQuery,
  useRemoveFavoriteMutation,
} from "@/features/favorites/hooks";
import {
  canShowFavoriteToggle,
  favoriteHeartFill,
  favoriteToggleLabel,
  favoriteToggleShowsHeart,
  type FavoriteToggleVariant,
} from "@/features/favorites/presentation";
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
  variant = "icon",
}: {
  productId: string;
  className?: string;
  variant?: FavoriteToggleVariant;
}) {
  const { session } = useSession();
  const router = useRouter();
  const favoritesQuery = useFavoritesQuery();
  const addMutation = useAddFavoriteMutation();
  const removeMutation = useRemoveFavoriteMutation();
  const favorited = isProductInFavorites(favoritesQuery.data?.items, productId);
  const mutating = addMutation.isPending || removeMutation.isPending;
  const showHeart = favoriteToggleShowsHeart(variant);
  const [pulse, setPulse] = useState(false);

  if (!canShowFavoriteToggle(session?.role)) {
    return null;
  }

  async function onToggle(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    event.stopPropagation();
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
      setPulse(true);
      window.setTimeout(() => setPulse(false), 220);
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
  const label = favoriteToggleLabel(favorited);

  if (!showHeart) {
    return (
      <div className={cx("grid gap-2", className)}>
        {errorMessage ? (
          <Alert tone="error" title="No se pudo actualizar">
            {errorMessage}
          </Alert>
        ) : null}
        <Button
          type="button"
          variant="ghost"
          onClick={onToggle}
          disabled={mutating}
          aria-pressed={favorited}
          aria-label={label}
          className="min-h-11 justify-start px-0 text-sm font-semibold text-sf-muted hover:bg-transparent hover:text-sf-error"
        >
          {label}
        </Button>
      </div>
    );
  }

  return (
    <div className={cx(className)}>
      <button
        type="button"
        onClick={onToggle}
        disabled={mutating}
        aria-pressed={favorited}
        aria-label={label}
        className={cx(
          "group relative inline-flex h-11 w-11 cursor-pointer items-center justify-center rounded-full",
          "border bg-sf-surface/90 shadow-[0_1px_2px_rgba(23,33,27,0.08)] backdrop-blur-sm",
          "transition-[background-color,border-color,box-shadow,transform] duration-200 ease-out",
          "hover:bg-sf-surface hover:shadow-[0_2px_8px_rgba(23,33,27,0.1)]",
          "active:scale-95",
          "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
          "disabled:cursor-not-allowed disabled:opacity-70",
          favorited
            ? "border-sf-error/40 text-sf-error"
            : "border-sf-border text-sf-error/75 hover:border-sf-error/35 hover:text-sf-error",
        )}
      >
        <HeartIcon
          className={cx(
            "transition-[transform,fill,opacity] duration-200 ease-out",
            "md:group-hover:scale-[1.09]",
            pulse && "scale-[1.12]",
            favorited ? "opacity-100" : "opacity-90",
          )}
          fill={favoriteHeartFill(favorited)}
        />
        <span
          className={cx(
            "pointer-events-none absolute right-0 top-[calc(100%+0.25rem)] z-30",
            "hidden whitespace-nowrap rounded-lg bg-sf-ink px-2 py-1 text-xs font-semibold text-white",
            "md:group-hover:block md:group-focus-visible:block",
          )}
        >
          {label}
        </span>
      </button>
      {errorMessage ? (
        <p
          role="alert"
          className="absolute right-0 top-12 z-20 mt-1 w-44 rounded-lg border border-sf-error/40 bg-red-50 px-2 py-1 text-xs text-sf-error"
        >
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
