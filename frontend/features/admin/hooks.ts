"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateAdminCategory,
  activateAdminProduct,
  adminKeys,
  changeAdminProductPrice,
  createAdminCategory,
  createAdminProduct,
  deactivateAdminCategory,
  deactivateAdminProduct,
  getAdminCategory,
  getAdminProduct,
  listAdminCategories,
  listAdminOrders,
  listAdminProducts,
  getAdminOrder,
  searchAdminProducts,
  updateAdminCategory,
  updateAdminProduct,
  type ChangeAdminProductPriceRequest,
  type CreateAdminCategoryRequest,
  type CreateAdminProductRequest,
  type ListAdminOrdersQuery,
  type ListAdminProductsQuery,
  type UpdateAdminCategoryRequest,
  type UpdateAdminProductRequest,
} from "@/features/admin/api";
import { isAdminRole, shouldSearchAdminProducts } from "@/features/admin/presentation";
import { useSession } from "@/shared/session/session-provider";

const keys = adminKeys();

export function useAdminCategoriesQuery() {
  const { session } = useSession();

  return useQuery({
    queryKey: keys.categories(),
    queryFn: listAdminCategories,
    enabled: isAdminRole(session?.role),
  });
}

export function useAdminCategoryQuery(categoryId: string) {
  const { session } = useSession();

  return useQuery({
    queryKey: keys.category(categoryId),
    queryFn: () => getAdminCategory(categoryId),
    enabled: isAdminRole(session?.role) && categoryId.length > 0,
  });
}

export function useCreateAdminCategoryMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: CreateAdminCategoryRequest) => createAdminCategory(body),
    onSuccess: (category) => {
      queryClient.setQueryData(keys.category(category.id), category);
      void queryClient.invalidateQueries({ queryKey: keys.categoriesRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.category(category.id) });
    },
  });
}

export function useUpdateAdminCategoryMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      categoryId,
      body,
    }: {
      categoryId: string;
      body: UpdateAdminCategoryRequest;
    }) => updateAdminCategory(categoryId, body),
    onSuccess: (category) => {
      queryClient.setQueryData(keys.category(category.id), category);
      void queryClient.invalidateQueries({ queryKey: keys.categoriesRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.category(category.id) });
    },
  });
}

export function useActivateAdminCategoryMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (categoryId: string) => activateAdminCategory(categoryId),
    onSuccess: (category) => {
      queryClient.setQueryData(keys.category(category.id), category);
      void queryClient.invalidateQueries({ queryKey: keys.categoriesRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.category(category.id) });
    },
  });
}

export function useDeactivateAdminCategoryMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (categoryId: string) => deactivateAdminCategory(categoryId),
    onSuccess: (category) => {
      queryClient.setQueryData(keys.category(category.id), category);
      void queryClient.invalidateQueries({ queryKey: keys.categoriesRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.category(category.id) });
    },
  });
}

export function useAdminProductsQuery(query: ListAdminProductsQuery & { text?: string }) {
  const { session } = useSession();
  const text = query.text?.trim() ?? "";
  const searching = shouldSearchAdminProducts(text);
  const listQuery: ListAdminProductsQuery = searching
    ? {}
    : { categoryId: query.categoryId, status: query.status };

  return useQuery({
    queryKey: searching ? keys.search(text) : keys.products(listQuery),
    queryFn: () =>
      searching
        ? searchAdminProducts({ text })
        : listAdminProducts(listQuery),
    enabled: isAdminRole(session?.role),
  });
}

export function useAdminProductQuery(productId: string) {
  const { session } = useSession();

  return useQuery({
    queryKey: keys.product(productId),
    queryFn: () => getAdminProduct(productId),
    enabled: isAdminRole(session?.role) && productId.length > 0,
  });
}

export function useCreateAdminProductMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: CreateAdminProductRequest) => createAdminProduct(body),
    onSuccess: (product) => {
      queryClient.setQueryData(keys.product(product.id), product);
      void queryClient.invalidateQueries({ queryKey: keys.productsRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.product(product.id) });
    },
  });
}

export function useUpdateAdminProductMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      productId,
      body,
    }: {
      productId: string;
      body: UpdateAdminProductRequest;
    }) => updateAdminProduct(productId, body),
    onSuccess: (product) => {
      queryClient.setQueryData(keys.product(product.id), product);
      void queryClient.invalidateQueries({ queryKey: keys.productsRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.product(product.id) });
    },
  });
}

export function useChangeAdminProductPriceMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      productId,
      body,
    }: {
      productId: string;
      body: ChangeAdminProductPriceRequest;
    }) => changeAdminProductPrice(productId, body),
    onSuccess: (product) => {
      queryClient.setQueryData(keys.product(product.id), product);
      void queryClient.invalidateQueries({ queryKey: keys.productsRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.product(product.id) });
    },
  });
}

export function useActivateAdminProductMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => activateAdminProduct(productId),
    onSuccess: (product) => {
      queryClient.setQueryData(keys.product(product.id), product);
      void queryClient.invalidateQueries({ queryKey: keys.productsRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.product(product.id) });
    },
  });
}

export function useDeactivateAdminProductMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => deactivateAdminProduct(productId),
    onSuccess: (product) => {
      queryClient.setQueryData(keys.product(product.id), product);
      void queryClient.invalidateQueries({ queryKey: keys.productsRoot() });
      void queryClient.invalidateQueries({ queryKey: keys.product(product.id) });
    },
  });
}

export function useAdminOrdersQuery(query: ListAdminOrdersQuery) {
  const { session } = useSession();

  return useQuery({
    queryKey: keys.orders(query),
    queryFn: () => listAdminOrders(query),
    enabled: isAdminRole(session?.role),
  });
}

export function useAdminOrderQuery(orderId: string) {
  const { session } = useSession();

  return useQuery({
    queryKey: keys.order(orderId),
    queryFn: () => getAdminOrder(orderId),
    enabled: isAdminRole(session?.role) && orderId.length > 0,
  });
}

