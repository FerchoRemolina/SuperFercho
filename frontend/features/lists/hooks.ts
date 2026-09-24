"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addShoppingListItem,
  changeShoppingListItemQuantity,
  clearShoppingList,
  createShoppingList,
  deleteShoppingList,
  getShoppingList,
  listShoppingLists,
  removeShoppingListItem,
  renameShoppingList,
  shoppingListsKeys,
  type AddShoppingListItemRequest,
  type ShoppingListNameRequest,
} from "@/features/lists/api";
import { useSession } from "@/shared/session/session-provider";

const keys = shoppingListsKeys();

export function useShoppingListsQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: keys.root(),
    queryFn: listShoppingLists,
    enabled,
  });
}

export function useShoppingListQuery(shoppingListId: string) {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER" && shoppingListId.length > 0;

  return useQuery({
    queryKey: keys.detail(shoppingListId),
    queryFn: () => getShoppingList(shoppingListId),
    enabled,
  });
}

export function useCreateShoppingListMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ShoppingListNameRequest) => createShoppingList(body),
    onSuccess: (list) => {
      void queryClient.invalidateQueries({ queryKey: keys.root() });
      queryClient.setQueryData(keys.detail(list.id), list);
    },
  });
}

export function useRenameShoppingListMutation(shoppingListId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ShoppingListNameRequest) =>
      renameShoppingList(shoppingListId, body),
    onSuccess: (list) => {
      queryClient.setQueryData(keys.detail(list.id), list);
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useAddShoppingListItemMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      shoppingListId,
      body,
    }: {
      shoppingListId: string;
      body: AddShoppingListItemRequest;
    }) => addShoppingListItem(shoppingListId, body),
    onSuccess: (list) => {
      queryClient.setQueryData(keys.detail(list.id), list);
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useChangeShoppingListItemQuantityMutation(
  shoppingListId: string,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      productId,
      quantity,
    }: {
      productId: string;
      quantity: number;
    }) =>
      changeShoppingListItemQuantity(shoppingListId, productId, { quantity }),
    onSuccess: (list) => {
      queryClient.setQueryData(keys.detail(list.id), list);
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useRemoveShoppingListItemMutation(shoppingListId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) =>
      removeShoppingListItem(shoppingListId, productId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: keys.detail(shoppingListId),
      });
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useClearShoppingListMutation(shoppingListId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => clearShoppingList(shoppingListId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: keys.detail(shoppingListId),
      });
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}

export function useDeleteShoppingListMutation(shoppingListId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => deleteShoppingList(shoppingListId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: keys.detail(shoppingListId) });
      void queryClient.invalidateQueries({ queryKey: keys.root() });
    },
  });
}
