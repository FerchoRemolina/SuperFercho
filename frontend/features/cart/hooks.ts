"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addProductToCart,
  cartKeys,
  changeCartItemQuantity,
  clearCart,
  getCart,
  removeCartItem,
  type AddCartItemRequest,
} from "@/features/cart/api";
import { useSession } from "@/shared/session/session-provider";

const keys = cartKeys();

export function useCartQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: keys.root(),
    queryFn: getCart,
    enabled,
  });
}

export function useAddCartItemMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: AddCartItemRequest) => addProductToCart(body),
    onSuccess: (cart) => {
      queryClient.setQueryData(keys.root(), cart);
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useChangeCartItemQuantityMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      productId,
      quantity,
    }: {
      productId: string;
      quantity: number;
    }) => changeCartItemQuantity(productId, { quantity }),
    onSuccess: (cart) => {
      queryClient.setQueryData(keys.root(), cart);
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useRemoveCartItemMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => removeCartItem(productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}

export function useClearCartMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: clearCart,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}
