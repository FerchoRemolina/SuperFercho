import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { orderStatusBadgeClassName, orderStatusTone } from "@/features/orders/order-views";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("orders presentation", () => {
  it("compacts OrderCard hierarchy around number, status, and total", () => {
    const card = source("features/orders/components/order-card.tsx");
    expect(card).toContain("order.orderNumber");
    expect(card).toContain("OrderStatusBadge");
    expect(card).toContain("formatMoney(order.total)");
    expect(card).toContain("formatOrderDate");
    expect(card).toContain("Ver detalle");
    expect(card).not.toContain("Número de pedido");
    expect(card).toContain('href={orderDetailHref(order.id)}');
  });

  it("removes Estado prefix and differentiates status tones", () => {
    const badge = source("features/orders/components/order-status-badge.tsx");
    expect(badge).not.toContain("Estado:");
    expect(badge).toContain("orderStatusLabel(status)");
    expect(orderStatusTone("PENDING")).toBe("accent");
    expect(orderStatusTone("CONFIRMED")).toBe("neutral");
    expect(orderStatusTone("PREPARING")).toBe("primary");
    expect(orderStatusTone("READY")).toBe("primary");
    expect(orderStatusTone("DELIVERED")).toBe("primary");
    expect(orderStatusTone("CANCELLED")).toBe("danger");
    expect(orderStatusBadgeClassName("READY")).toContain("ring-");
    expect(orderStatusBadgeClassName("DELIVERED")).toContain("sf-primary");
  });

  it("prioritizes products and softens detail chrome", () => {
    const detail = source("features/orders/components/order-result-content.tsx");
    expect(detail).toContain("← Pedidos");
    expect(detail).toContain('href="/orders"');
    expect(detail).toContain("Productos");
    expect(detail).toContain("line-clamp-2");
    expect(detail).toContain("Cantidad:");
    expect(detail).toContain("c/u");
    expect(detail).toContain("Seguir comprando");
    expect(detail).toContain('buttonClassName("secondary")');
    expect(detail).not.toContain('buttonClassName("primary")');
    expect(detail).not.toContain(">Creado<");
    expect(detail).not.toContain("Número de pedido");
  });

  it("keeps cancel panel logic intact while using Card presentation", () => {
    const cancel = source("features/orders/components/cancel-order-panel.tsx");
    expect(cancel).toContain("cancelPanelState");
    expect(cancel).toContain("Cancelar pedido");
    expect(cancel).toContain("Conservar pedido");
    expect(cancel).toContain("CANCEL_WINDOW_EXPIRED_COPY");
    expect(cancel).toContain("<Card");
    expect(cancel).not.toContain("border-dashed");
  });
});
