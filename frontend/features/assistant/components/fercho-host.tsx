"use client";

import { AssistantChat } from "@/features/assistant/components/assistant-chat";
import { useFerchoShell } from "@/features/assistant/fercho-shell-context";
import { useSession } from "@/shared/session/session-provider";
import { cx } from "@/shared/utils/cx";
import { usePathname } from "next/navigation";

/**
 * Single mounted AssistantChat for the storefront. Visibility (page / panel /
 * hidden) never posts chat turns or cart mutations.
 */
export function FerchoHost() {
  const pathname = usePathname();
  const { session } = useSession();
  const { isOpen, isExpanded, minimize, toggleExpanded } = useFerchoShell();

  if (session?.role !== "CUSTOMER") {
    return null;
  }

  const isAssistantPage = pathname.startsWith("/assistant");
  const isCheckout = pathname.startsWith("/checkout");
  const showUi = isAssistantPage || (isOpen && !isCheckout);
  const panelExpanded = !isAssistantPage && isExpanded;
  const dockedChrome =
    "fixed inset-0 z-50 flex flex-col bg-sf-surface pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]";
  const dockedPanel =
    "md:inset-auto md:bottom-24 md:right-6 md:h-[min(40rem,calc(100dvh-8rem))] md:w-[min(24rem,calc(100vw-2rem))] md:rounded-xl md:border md:border-sf-border md:bg-sf-surface md:shadow-[0_8px_24px_rgba(23,33,27,0.18)] md:pt-0 md:pb-0";

  return (
    <div
      id="fercho-shell"
      className={cx(
        !showUi && "hidden",
        isAssistantPage && "relative",
        showUi && !isAssistantPage && dockedChrome,
        showUi && !isAssistantPage && !panelExpanded && dockedPanel,
      )}
    >
      <AssistantChat
        variant={isAssistantPage ? "page" : "docked"}
        isExpanded={panelExpanded}
        onToggleExpanded={isAssistantPage ? undefined : toggleExpanded}
        onMinimize={isAssistantPage ? undefined : minimize}
      />
    </div>
  );
}
