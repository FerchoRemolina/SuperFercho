"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useMemo, useState, type ComponentType, type SVGProps } from "react";
import {
  ADMIN_NAV_TREE,
  type AdminNavGroup,
  type AdminNavNode,
  isAdminNavActive,
  isAdminNavNodeActive,
} from "@/features/admin/presentation";
import { BrandLogo } from "@/shared/ui/brand-logo";
import {
  BookIcon,
  ChevronIcon,
  ClipboardListIcon,
  PackageIcon,
  UserIcon,
} from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

type IconComponent = ComponentType<SVGProps<SVGSVGElement>>;

function HomeNavIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      width="1.25em"
      height="1.25em"
      {...props}
    >
      <path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9.5z" />
    </svg>
  );
}

const ROOT_ICONS: Record<string, IconComponent> = {
  "/admin": HomeNavIcon,
  inventory: PackageIcon,
  sales: ClipboardListIcon,
  customers: UserIcon,
  fercho: BookIcon,
};

function collectRouteExpanded(pathname: string): Record<string, boolean> {
  const open: Record<string, boolean> = {};
  function walk(node: AdminNavNode) {
    if (node.kind !== "group") {
      return;
    }
    if (isAdminNavNodeActive(node, pathname)) {
      open[node.id] = true;
    }
    for (const child of node.children) {
      walk(child);
    }
  }
  for (const node of ADMIN_NAV_TREE) {
    walk(node);
  }
  return open;
}

export function AdminSidebar({
  inventoryAttentionCount = 0,
  mobileOpen,
  onCloseMobile,
}: {
  inventoryAttentionCount?: number;
  mobileOpen?: boolean;
  onCloseMobile?: () => void;
}) {
  const pathname = usePathname();
  const routeExpanded = useMemo(
    () => collectRouteExpanded(pathname),
    [pathname],
  );
  const [manualExpanded, setManualExpanded] = useState<
    Record<string, boolean | undefined>
  >({});

  function isExpanded(id: string): boolean {
    const manual = manualExpanded[id];
    if (manual !== undefined) {
      return manual;
    }
    return Boolean(routeExpanded[id]);
  }

  function toggleGroup(id: string) {
    setManualExpanded((prev) => ({
      ...prev,
      [id]: !isExpanded(id),
    }));
  }

  return (
    <>
      <div
        className={cx(
          "fixed inset-0 z-40 bg-sf-ink/30 transition-opacity duration-200 lg:hidden",
          mobileOpen ? "opacity-100" : "pointer-events-none opacity-0",
        )}
        aria-hidden={!mobileOpen}
        onClick={onCloseMobile}
      />
      <aside
        className={cx(
          "fixed inset-y-0 left-0 z-50 flex w-[16rem] flex-col border-r border-sf-border bg-sf-surface",
          "shadow-[0_1px_3px_rgba(16,24,40,0.04)] transition-transform duration-200 ease-out lg:translate-x-0",
          mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
        )}
        aria-label="Navegación administrativa"
      >
        <div className="flex h-16 shrink-0 items-center px-4">
          <BrandLogo href="/admin" className="min-h-0 [&_img]:w-[216px]" />
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <ul className="grid gap-1">
            {ADMIN_NAV_TREE.map((node) => (
              <li key={node.kind === "leaf" ? node.href : node.id}>
                <AdminNavItem
                  node={node}
                  pathname={pathname}
                  depth={0}
                  inventoryAttentionCount={inventoryAttentionCount}
                  isExpanded={isExpanded}
                  onToggle={toggleGroup}
                  onNavigate={onCloseMobile}
                />
              </li>
            ))}
          </ul>
        </nav>
      </aside>
    </>
  );
}

function AdminNavItem({
  node,
  pathname,
  depth,
  inventoryAttentionCount,
  isExpanded,
  onToggle,
  onNavigate,
}: {
  node: AdminNavNode;
  pathname: string;
  depth: number;
  inventoryAttentionCount: number;
  isExpanded: (id: string) => boolean;
  onToggle: (id: string) => void;
  onNavigate?: () => void;
}) {
  if (node.kind === "leaf") {
    const active = isAdminNavActive(node.href, pathname, node.match);
    const Icon = depth === 0 ? ROOT_ICONS[node.href] : undefined;
    return (
      <Link
        href={node.href}
        onClick={onNavigate}
        className={cx(
          "relative flex min-h-10 items-center gap-2.5 rounded-lg px-3 text-sm font-semibold transition-all duration-150",
          depth >= 1 && "pl-3",
          active
            ? "bg-sf-primary/10 text-sf-primary before:absolute before:inset-y-2 before:left-0 before:w-[3px] before:rounded-full before:bg-sf-primary"
            : "text-sf-muted hover:bg-sf-bg hover:text-sf-ink",
        )}
        aria-current={active ? "page" : undefined}
      >
        {Icon ? <Icon className="h-[18px] w-[18px] shrink-0" /> : null}
        <span className="truncate">{node.label}</span>
      </Link>
    );
  }

  return (
    <AdminNavGroupItem
      node={node}
      pathname={pathname}
      depth={depth}
      inventoryAttentionCount={inventoryAttentionCount}
      isExpanded={isExpanded}
      onToggle={onToggle}
      onNavigate={onNavigate}
    />
  );
}

function AdminNavGroupItem({
  node,
  pathname,
  depth,
  inventoryAttentionCount,
  isExpanded,
  onToggle,
  onNavigate,
}: {
  node: AdminNavGroup;
  pathname: string;
  depth: number;
  inventoryAttentionCount: number;
  isExpanded: (id: string) => boolean;
  onToggle: (id: string) => void;
  onNavigate?: () => void;
}) {
  const open = isExpanded(node.id);
  const groupActive = isAdminNavNodeActive(node, pathname);
  const badge =
    node.badgeKey === "inventoryAttention" && inventoryAttentionCount > 0
      ? inventoryAttentionCount
      : null;
  const Icon = depth === 0 ? ROOT_ICONS[node.id] : undefined;

  return (
    <div className={cx(depth > 0 && "ml-0.5")}>
      <button
        type="button"
        className={cx(
          "flex min-h-10 w-full items-center gap-2.5 rounded-lg px-3 text-left text-sm font-semibold transition-all duration-150",
          groupActive
            ? "bg-sf-primary/10 text-sf-primary"
            : open
              ? "bg-sf-bg text-sf-ink"
              : "text-sf-muted hover:bg-sf-bg hover:text-sf-ink",
        )}
        aria-expanded={open}
        onClick={() => onToggle(node.id)}
      >
        {Icon ? <Icon className="h-[18px] w-[18px] shrink-0" /> : null}
        <span className="min-w-0 flex-1 truncate">{node.label}</span>
        {badge != null ? (
          <span
            className="inline-flex min-w-5 items-center justify-center rounded-full bg-sf-warning/15 px-1.5 py-0.5 text-[10px] font-bold tabular-nums text-sf-warning transition-transform duration-150"
            aria-label={`${badge} productos requieren atención`}
          >
            {badge}
          </span>
        ) : null}
        <ChevronIcon
          className={cx(
            "h-3.5 w-3.5 shrink-0 opacity-70 transition-transform duration-200 ease-out",
            open ? "rotate-90" : "rotate-0",
          )}
        />
      </button>
      <div
        className={cx(
          "grid transition-[grid-template-rows] duration-200 ease-out",
          open ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
        )}
      >
        <div className="overflow-hidden">
          <ul className="mt-1 grid gap-0.5 border-l border-sf-border/80 ml-5 pl-2 pb-1">
            {node.children.map((child) => (
              <li key={child.kind === "leaf" ? child.href : child.id}>
                <AdminNavItem
                  node={child}
                  pathname={pathname}
                  depth={depth + 1}
                  inventoryAttentionCount={inventoryAttentionCount}
                  isExpanded={isExpanded}
                  onToggle={onToggle}
                  onNavigate={onNavigate}
                />
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
