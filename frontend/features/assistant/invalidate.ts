import type { QueryClient } from "@tanstack/react-query";
import { addressKeys } from "@/features/account/api";
import { cartKeys } from "@/features/cart/api";
import { shoppingListsKeys } from "@/features/lists/api";
import { orderKeys } from "@/features/orders/api";

/**
 * Assistant tools can mutate cart, lists, addresses, and orders without the
 * response naming which tools ran. Invalidate the existing customer query
 * roots after a successful chat turn.
 */
export function invalidateAfterAssistantChat(
  queryClient: QueryClient,
): void {
  void queryClient.invalidateQueries({ queryKey: cartKeys().root() });
  void queryClient.invalidateQueries({ queryKey: orderKeys().all });
  void queryClient.invalidateQueries({
    queryKey: shoppingListsKeys().root(),
  });
  void queryClient.invalidateQueries({ queryKey: addressKeys().root() });
}
