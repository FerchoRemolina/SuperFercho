"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addFavorite,
  favoritesKeys,
  getFavorites,
  removeFavorite,
} from "@/features/favorites/api";
import { useSession } from "@/shared/session/session-provider";

const keys = favoritesKeys();

export function useFavoritesQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: keys.root(),
    queryFn: getFavorites,
    enabled,
  });
}

export function useAddFavoriteMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => addFavorite(productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}

export function useRemoveFavoriteMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => removeFavorite(productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}
