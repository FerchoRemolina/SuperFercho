import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("fercho persistent storefront shell", () => {
  const layout = source("app/(storefront)/layout.tsx");
  const page = source("app/(storefront)/(customer)/assistant/page.tsx");
  const host = source("features/assistant/components/fercho-host.tsx");
  const fab = source("features/assistant/components/fercho-fab.tsx");
  const context = source("features/assistant/fercho-shell-context.tsx");
  const chat = source("features/assistant/components/assistant-chat.tsx");
  const header = source("features/assistant/components/fercho-panel-header.tsx");
  const customerLayout = source("app/(storefront)/(customer)/layout.tsx");

  it("mounts a single AssistantChat from the storefront layout host", () => {
    expect(layout).toContain("<FerchoHost />");
    expect(host).toContain("<AssistantChat");
    expect(host.split("<AssistantChat").length - 1).toBe(1);
    expect(page).not.toContain("AssistantChat");
    expect(layout).not.toContain("AssistantChat");
    expect(fab).not.toContain("AssistantChat");
  });

  it("opens and minimizes without posting chat or cart actions", () => {
    expect(context).toContain("setIsOpen(true)");
    expect(context).toContain("setIsOpen(false)");
    expect(context).not.toContain("postAssistantChat");
    expect(context).not.toContain("selectPackage");
    expect(context).not.toContain("addProductToCart");
    expect(fab).toContain("onClick={toggle}");
    expect(fab).not.toContain("postAssistantChat");
    expect(fab).not.toContain("addProductToCart");
    expect(host).toContain("onMinimize={isAssistantPage ? undefined : minimize}");
    expect(host).not.toContain("postAssistantChat");
    expect(host).not.toContain("addProductToCart");
    expect(header).toContain("onClick={onMinimize}");
    expect(header).toContain("onClick={onToggleExpanded}");
    expect(header).toContain("onClick={onNewConversation}");
  });

  it("keeps the host in the storefront layout so route changes do not remount chat", () => {
    expect(layout).toContain("FerchoShellProvider");
    expect(layout).toContain("<FerchoHost />");
    expect(host).toContain('session?.role !== "CUSTOMER"');
    expect(host).toContain('!showUi && "hidden"');
    expect(host).not.toContain("localStorage");
    expect(host).not.toContain("sessionStorage");
    expect(context).not.toContain("localStorage");
    expect(context).not.toContain("sessionStorage");
  });

  it("presents /assistant as the same instance expanded", () => {
    expect(host).toContain('pathname.startsWith("/assistant")');
    expect(host).toContain('variant={isAssistantPage ? "page" : "docked"}');
    expect(page).not.toContain("AssistantChat");
    expect(customerLayout).toContain("RequireRole");
    expect(customerLayout).toContain('role="CUSTOMER"');
  });

  it("toggles maximize/restore on the same AssistantChat instance", () => {
    expect(context).toContain("isExpanded");
    expect(context).toContain("toggleExpanded");
    expect(context).toContain("setIsExpanded");
    expect(host.split("<AssistantChat").length - 1).toBe(1);
    expect(host).toContain("onToggleExpanded={isAssistantPage ? undefined : toggleExpanded}");
    expect(host).toContain("panelExpanded");
    expect(chat).toContain("FerchoPanelHeader");
  });

  it("keeps a compact icon header: plus beside title, maximize then minus, no close", () => {
    const body = header.slice(header.indexOf("export function FerchoPanelHeader"));
    const ferchoIndex = body.indexOf("Fercho");
    const plusIndex = body.indexOf("PlusIcon");
    const maximizeIndex = body.indexOf("MaximizeIcon");
    const restoreIndex = body.indexOf("RestoreIcon");
    const minusIndex = body.indexOf("MinusIcon");
    expect(ferchoIndex).toBeGreaterThan(-1);
    expect(plusIndex).toBeGreaterThan(ferchoIndex);
    expect(Math.min(maximizeIndex, restoreIndex)).toBeGreaterThan(plusIndex);
    expect(minusIndex).toBeGreaterThan(Math.max(maximizeIndex, restoreIndex));
    expect(header).not.toContain("CloseIcon");
    expect(header).not.toContain("Cerrar");
    expect(header).toContain('label="Nueva conversación"');
    expect(header).toContain('tooltip="Nueva conversación"');
    expect(header).toContain("md:group-hover/icon:block");
    expect(header).not.toMatch(/<button[^>]*>\s*Nueva conversación\s*</);
  });

  it("hides the FAB on /assistant and /checkout and gates by CUSTOMER", () => {
    expect(fab).toContain('session?.role !== "CUSTOMER"');
    expect(fab).toContain('pathname.startsWith("/assistant")');
    expect(fab).toContain('pathname.startsWith("/checkout")');
    expect(fab).toContain("return null");
    expect(host).toContain('pathname.startsWith("/checkout")');
    expect(host).toContain("isOpen && !isCheckout");
  });

  it("resets local conversation state without reusing the previous conversationId", () => {
    expect(chat).toContain("function resetConversation");
    expect(chat).toContain("setConversationId(null)");
    expect(chat).toContain("setMessages([])");
    expect(chat).toContain("setPackageProposal(null)");
    expect(chat).toContain("setPendingConfirmation(null)");
    expect(chat).toContain("messageChatBody(conversationId, message)");
    expect(chat).not.toContain("localStorage");
    expect(chat).not.toContain("sessionStorage");
  });

  it("uses a docked fullscreen overlay on small screens and a panel from md", () => {
    expect(host).toContain("fixed inset-0");
    expect(host).toContain("md:bottom-24");
    expect(host).toContain("md:right-6");
    expect(fab).toContain("max-md:hidden");
  });
});
