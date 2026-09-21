"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cartKeys } from "@/features/cart/api";
import { catalogKeys } from "@/features/catalog/api";
import {
  cancelOrder,
  checkout,
  getOrder,
  listOrders,
  orderKeys,
  type CheckoutRequest,
} from "@/features/orders/api";
import { addressKeys } from "@/features/account/api";
import { isApiError } from "@/shared/errors/api-problem";
import { useSession } from "@/shared/session/session-provider";
import { cancelOrderCacheKeys } from "@/features/orders/order-views";

const keys = orderKeys();

export function useOrdersQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: keys.all,
    queryFn: () => listOrders(),
    enabled,
  });
}

export function useOrderQuery(orderId: string) {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER" && orderId.length > 0;

  return useQuery({
    queryKey: keys.detail(orderId),
    queryFn: () => getOrder(orderId),
    enabled,
  });
}

export function useCheckoutMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      body,
      idempotencyKey,
    }: {
      body: CheckoutRequest;
      idempotencyKey: string;
    }) => checkout(body, idempotencyKey),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cartKeys().root() });
      void queryClient.invalidateQueries({ queryKey: keys.all });
    },
    onError: (error) => {
      if (!isApiError(error) || !error.problem.code) {
        return;
      }
      const code = error.problem.code;
      if (
        code === "PRODUCT_PRICE_CHANGED" ||
        code === "PRODUCT_NOT_AVAILABLE" ||
        code === "STOCK_UNAVAILABLE" ||
        code === "CART_EMPTY"
      ) {
        void queryClient.invalidateQueries({ queryKey: cartKeys().root() });
        void queryClient.invalidateQueries({
          queryKey: catalogKeys().all,
        });
      }
      if (code === "ADDRESS_NOT_AVAILABLE" || code === "IDEMPOTENCY_CONFLICT") {
        void queryClient.invalidateQueries({ queryKey: addressKeys().root() });
      }
      if (code === "IDEMPOTENCY_CONFLICT") {
        void queryClient.invalidateQueries({ queryKey: cartKeys().root() });
        void queryClient.invalidateQueries({
          queryKey: catalogKeys().all,
        });
      }
    },
  });
}

export function useCancelOrderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (orderId: string) => cancelOrder(orderId),
    onSuccess: async (order) => {
      const cache = cancelOrderCacheKeys(order.id);
      queryClient.setQueryData(cache.detail, order);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: cache.list }),
        queryClient.invalidateQueries({ queryKey: cache.detail }),
      ]);
    },
    onError: (error, orderId) => {
      if (
        !isApiError(error) ||
        (error.problem.code !== "CANCELLATION_NOT_ALLOWED" &&
          error.problem.code !== "INVALID_ORDER_TRANSITION")
      ) {
        return;
      }
      const cache = cancelOrderCacheKeys(orderId);
      void queryClient.invalidateQueries({ queryKey: cache.list });
      void queryClient.invalidateQueries({ queryKey: cache.detail });
    },
  });
}
