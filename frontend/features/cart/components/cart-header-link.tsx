"use client";

import Link from "next/link";
import { cartUnitCount } from "@/features/cart/api";
import { useCartQuery } from "@/features/cart/hooks";
import { useSession } from "@/shared/session/session-provider";
import { buttonClassName } from "@/shared/ui/button";
import { CartIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

export function CartHeaderLink({ compact = false }: { compact?: boolean }) {
  const { session } = useSession();
  const cartQuery = useCartQuery();
  const isCustomer = session?.role === "CUSTOMER";
  const count = isCustomer ? cartUnitCount(cartQuery.data) : 0;

  return (
    <Link
      href="/cart"
      aria-label={
        count > 0 ? `Carrito, ${count} unidades` : "Carrito"
      }
      className={buttonClassName("ghost", compact ? "relative px-3" : "relative gap-2")}
    >
      <CartIcon />
      {compact ? null : "Carrito"}
      {count > 0 ? (
        <span
          className={cx(
            "inline-flex min-h-5 min-w-5 items-center justify-center rounded-full bg-sf-yellow px-1.5 text-xs font-bold text-sf-ink",
            compact ? "absolute -right-0.5 -top-0.5" : "",
          )}
        >
          {count}
        </span>
      ) : null}
    </Link>
  );
}
