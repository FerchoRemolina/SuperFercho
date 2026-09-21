"use client";

import { useQuery } from "@tanstack/react-query";
import {
  catalogKeys,
  getCategory,
  getProduct,
  listCategories,
  listProducts,
  searchProducts,
} from "@/features/catalog/api";

const keys = catalogKeys();

export function useCategoriesQuery() {
  return useQuery({
    queryKey: keys.categories(),
    queryFn: listCategories,
  });
}

export function useCategoryQuery(categoryId: string) {
  return useQuery({
    queryKey: keys.category(categoryId),
    queryFn: () => getCategory(categoryId),
    enabled: categoryId.length > 0,
  });
}

export function useProductsQuery(categoryId?: string) {
  return useQuery({
    queryKey: keys.products(categoryId),
    queryFn: () => listProducts({ categoryId }),
  });
}

export function useProductQuery(productId: string) {
  return useQuery({
    queryKey: keys.product(productId),
    queryFn: () => getProduct(productId),
    enabled: productId.length > 0,
  });
}

export function useSearchProductsQuery(text: string) {
  const trimmed = text.trim();
  return useQuery({
    queryKey: keys.search(trimmed),
    queryFn: () => searchProducts({ text: trimmed }),
    enabled: trimmed.length > 0,
  });
}
