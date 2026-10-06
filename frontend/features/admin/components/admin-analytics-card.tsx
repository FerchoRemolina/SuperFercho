"use client";

import {
  useEffect,
  useId,
  useRef,
  useState,
  type ComponentType,
  type ReactNode,
  type SVGProps,
} from "react";
import {
  useAdminAnalyticsNewCustomersQuery,
  useAdminAnalyticsSalesQuery,
  useAdminAnalyticsTopCustomersQuery,
  useAdminAnalyticsTopProductsQuery,
} from "@/features/admin/hooks";
import {
  analyticsPresetLabel,
  analyticsSalesKpis,
  analyticsShortRangeLabel,
  isValidAnalyticsDateInput,
  resolveAnalyticsPeriod,
  type AnalyticsGranularity,
  type AnalyticsPresetId,
  type AnalyticsPeriodSelection,
} from "@/features/admin/analytics-period";
import { formatMoney } from "@/shared/money/money";
import {
  CartIcon,
  CheckCircleIcon,
  ChevronIcon,
  ClipboardListIcon,
  PackageIcon,
  RefreshIcon,
  SparklesIcon,
  TagIcon,
  UserIcon,
} from "@/shared/ui/icons";
import { Card } from "@/shared/ui/card";
import { Skeleton } from "@/shared/ui/skeleton";
import { cx } from "@/shared/utils/cx";

type IconComponent = ComponentType<SVGProps<SVGSVGElement>>;

type SeriesMetricId = "sales" | "orders" | "newCustomers";
type RankingMetricId = "topProducts" | "lowProducts" | "topCustomers";
type AnalyticsMetricId = SeriesMetricId | RankingMetricId;

const ANALYTICS_METRICS = [
  { id: "sales", label: "Ventas", icon: CartIcon, kind: "series" },
  { id: "orders", label: "Pedidos", icon: ClipboardListIcon, kind: "series" },
  { id: "newCustomers", label: "Clientes nuevos", icon: SparklesIcon, kind: "series" },
  { id: "topProducts", label: "Más vendidos", icon: PackageIcon, kind: "topProducts" },
  { id: "lowProducts", label: "Menos vendidos", icon: TagIcon, kind: "lowProducts" },
  { id: "topCustomers", label: "Mejores clientes", icon: UserIcon, kind: "topCustomers" },
] as const satisfies readonly {
  id: AnalyticsMetricId;
  label: string;
  icon: IconComponent;
  kind: "series" | RankingMetricId;
}[];

const SELECTOR_CALENDAR_PRESETS = [
  "today",
  "thisWeek",
  "thisMonth",
  "thisYear",
] as const satisfies readonly AnalyticsPresetId[];

/** Colores contextuales por métrica (sutiles; el azul SuperFercho manda). */
const METRIC_TONES: Record<
  AnalyticsMetricId,
  { text: string; tile: string; bar: string; stroke: string }
> = {
  sales: { text: "text-sf-primary", tile: "bg-sf-primary/10", bar: "bg-sf-primary", stroke: "#1d6ceb" },
  orders: { text: "text-indigo-600", tile: "bg-indigo-500/10", bar: "bg-indigo-500", stroke: "#6366f1" },
  newCustomers: { text: "text-teal-600", tile: "bg-teal-500/10", bar: "bg-teal-500", stroke: "#14b8a6" },
  topProducts: { text: "text-emerald-600", tile: "bg-emerald-500/10", bar: "bg-emerald-500", stroke: "#10b981" },
  lowProducts: { text: "text-amber-600", tile: "bg-amber-500/10", bar: "bg-amber-500", stroke: "#f59e0b" },
  topCustomers: { text: "text-violet-600", tile: "bg-violet-500/10", bar: "bg-violet-500", stroke: "#8b5cf6" },
};

function analyticsMoney(amount: number): string {
  return formatMoney({ amount, currency: "COP" });
}

function moneyValue(money: { amount: number | string }): number {
  return typeof money.amount === "number" ? money.amount : Number(money.amount);
}

/** `id` es el periodStart del bucket (único por construcción del backend);
 *  `label` es solo presentación (ej. "HH:mm" se repite en rangos >24h). */
type SeriesPoint = { id: string; label: string; value: number };

export function AnalyticsCard() {
  const [metricId, setMetricId] = useState<AnalyticsMetricId>("sales");
  const [periodSelection, setPeriodSelection] = useState<AnalyticsPeriodSelection>({
    kind: "preset",
    id: "thisMonth",
  });
  const [customFormOpen, setCustomFormOpen] = useState(false);
  const [customFromDraft, setCustomFromDraft] = useState("");
  const [customToDraft, setCustomToDraft] = useState("");

  const metric = ANALYTICS_METRICS.find((m) => m.id === metricId)!;
  const tone = METRIC_TONES[metricId];
  const resolvedPeriod = resolveAnalyticsPeriod(periodSelection);
  const isSeries = metric.kind === "series";

  const salesSeriesQuery = useAdminAnalyticsSalesQuery(
    resolvedPeriod.from,
    resolvedPeriod.to,
    resolvedPeriod.granularity,
    isSeries,
  );
  const newCustomersQuery = useAdminAnalyticsNewCustomersQuery(
    resolvedPeriod.from,
    resolvedPeriod.to,
    resolvedPeriod.granularity,
    metricId === "newCustomers",
  );
  const topProductsQuery = useAdminAnalyticsTopProductsQuery(
    resolvedPeriod.from,
    resolvedPeriod.to,
    metricId === "lowProducts" ? "ASC" : "DESC",
    metricId === "topProducts" || metricId === "lowProducts",
  );
  const topCustomersQuery = useAdminAnalyticsTopCustomersQuery(
    resolvedPeriod.from,
    resolvedPeriod.to,
    metricId === "topCustomers",
  );

  const periodMenuId = useId();
  const periodMenuRef = useRef<HTMLDivElement>(null);
  const [periodMenuOpen, setPeriodMenuOpen] = useState(false);

  useEffect(() => {
    if (!periodMenuOpen) {
      return;
    }
    function onPointerDown(event: MouseEvent) {
      if (!periodMenuRef.current?.contains(event.target as Node)) {
        setPeriodMenuOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setPeriodMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [periodMenuOpen]);

  const periodButtonLabel =
    periodSelection.kind === "custom"
      ? analyticsShortRangeLabel(periodSelection.from, periodSelection.to)
      : customFormOpen
        ? "Personalizado"
        : resolvedPeriod.label;

  const customDraftValid =
    isValidAnalyticsDateInput(customFromDraft) &&
    isValidAnalyticsDateInput(customToDraft) &&
    customFromDraft <= customToDraft;

  function openCustomForm() {
    if (periodSelection.kind === "custom") {
      setCustomFromDraft(periodSelection.from);
      setCustomToDraft(periodSelection.to);
    } else {
      setCustomFromDraft("");
      setCustomToDraft("");
    }
    setCustomFormOpen(true);
    setPeriodMenuOpen(false);
  }

  function applyCustomRange() {
    if (!customDraftValid) {
      return;
    }
    setPeriodSelection({
      kind: "custom",
      from: customFromDraft,
      to: customToDraft,
    });
    setCustomFormOpen(false);
  }

  return (
    <section aria-labelledby="admin-analytics-heading">
      <Card className="overflow-hidden rounded-xl border-sf-border p-0 shadow-[0_1px_2px_rgba(16,24,40,0.05)]">
        <div className="flex flex-col gap-3 border-b border-sf-border px-4 py-3 md:flex-row md:items-center md:justify-between md:px-5">
          <div className="flex items-center gap-3">
            <span
              className={cx(
                "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg",
                tone.tile,
                tone.text,
              )}
            >
              <metric.icon className="h-4 w-4" />
            </span>
            <div>
              <h2
                id="admin-analytics-heading"
                className="text-base font-bold tracking-tight text-sf-ink"
              >
                Analítica
              </h2>
              <p className="mt-0.5 text-xs text-sf-muted">{resolvedPeriod.label}</p>
            </div>
          </div>

          <div className="relative" ref={periodMenuRef}>
            <button
              type="button"
              aria-haspopup="menu"
              aria-expanded={periodMenuOpen}
              aria-controls={periodMenuId}
              onClick={() => setPeriodMenuOpen((open) => !open)}
              className={cx(
                "inline-flex min-h-8 items-center gap-1.5 whitespace-nowrap rounded-lg border border-sf-border bg-sf-surface px-3 text-sm font-semibold text-sf-ink transition-colors hover:bg-sf-bg",
                "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
              )}
            >
              {periodButtonLabel}
              <ChevronIcon
                className={cx(
                  "h-3.5 w-3.5 text-sf-muted transition-transform duration-200",
                  periodMenuOpen && "rotate-90",
                )}
              />
            </button>
            {periodMenuOpen ? (
              <div
                id={periodMenuId}
                role="menu"
                aria-label="Período de análisis"
                className="absolute right-0 z-20 mt-2 w-56 overflow-hidden rounded-xl border border-sf-border bg-sf-surface py-1 shadow-[0_8px_24px_rgba(16,24,40,0.10)]"
              >
                {SELECTOR_CALENDAR_PRESETS.map((preset) => {
                  const active =
                    periodSelection.kind === "preset" &&
                    periodSelection.id === preset;
                  return (
                    <button
                      key={preset}
                      type="button"
                      role="menuitem"
                      className={cx(
                        "flex w-full min-h-9 items-center justify-between gap-2 px-3 text-left text-sm transition-colors hover:bg-sf-bg",
                        active ? "font-semibold text-sf-primary" : "text-sf-ink",
                      )}
                      onClick={() => {
                        setPeriodSelection({ kind: "preset", id: preset });
                        setCustomFormOpen(false);
                        setPeriodMenuOpen(false);
                      }}
                    >
                      {analyticsPresetLabel(preset)}
                      {active ? <CheckCircleIcon className="h-4 w-4 shrink-0" /> : null}
                    </button>
                  );
                })}
                <button
                  type="button"
                  role="menuitem"
                  className={cx(
                    "flex w-full min-h-9 items-center justify-between gap-2 px-3 text-left text-sm transition-colors hover:bg-sf-bg",
                    periodSelection.kind === "custom"
                      ? "font-semibold text-sf-primary"
                      : "text-sf-ink",
                  )}
                  onClick={openCustomForm}
                >
                  Personalizado
                  {periodSelection.kind === "custom" ? (
                    <CheckCircleIcon className="h-4 w-4 shrink-0" />
                  ) : null}
                </button>
              </div>
            ) : null}
          </div>
        </div>

        {customFormOpen ? (
          <div className="flex flex-wrap items-end gap-3 border-b border-sf-border px-4 py-2.5 md:px-5">
            <label className="grid gap-1 text-xs font-semibold text-sf-ink">
              Desde
              <input
                type="date"
                value={customFromDraft}
                onChange={(event) => setCustomFromDraft(event.target.value)}
                className="min-h-8 rounded-lg border border-sf-border bg-sf-surface px-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
              />
            </label>
            <label className="grid gap-1 text-xs font-semibold text-sf-ink">
              Hasta
              <input
                type="date"
                value={customToDraft}
                onChange={(event) => setCustomToDraft(event.target.value)}
                className="min-h-8 rounded-lg border border-sf-border bg-sf-surface px-2 text-sm text-sf-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-sf-primary"
              />
            </label>
            <button
              type="button"
              disabled={!customDraftValid}
              onClick={applyCustomRange}
              className={cx(
                "min-h-8 rounded-lg px-3.5 text-sm font-semibold transition-colors",
                customDraftValid
                  ? "bg-sf-primary text-white hover:bg-sf-primary-hover"
                  : "cursor-not-allowed bg-sf-border text-sf-muted",
              )}
            >
              Aplicar
            </button>
          </div>
        ) : null}

        <div className="border-b border-sf-border px-3 py-2 md:px-5">
          <div
            className="-mx-1 flex gap-1 overflow-x-auto px-1 pb-1"
            role="group"
            aria-label="Métrica de análisis"
          >
            {ANALYTICS_METRICS.map((option) => {
              const active = option.id === metricId;
              return (
                <button
                  key={option.id}
                  type="button"
                  aria-pressed={active}
                  className={cx(
                    "inline-flex min-h-8 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-lg px-3 text-sm font-semibold transition-all duration-150",
                    active
                      ? cx("bg-sf-bg", METRIC_TONES[option.id].text)
                      : "text-sf-muted hover:text-sf-ink",
                  )}
                  onClick={() => setMetricId(option.id)}
                >
                  <option.icon className="h-4 w-4" />
                  {option.label}
                </button>
              );
            })}
          </div>
        </div>

        <AnalyticsBody
          metricId={metricId}
          periodLabel={resolvedPeriod.label}
          granularity={resolvedPeriod.granularity}
          isSeries={isSeries}
          salesSeriesQuery={salesSeriesQuery}
          newCustomersQuery={newCustomersQuery}
          topProductsQuery={topProductsQuery}
          topCustomersQuery={topCustomersQuery}
          tone={tone}
        />
      </Card>
    </section>
  );
}

function AnalyticsBody({
  metricId,
  periodLabel,
  granularity,
  isSeries,
  salesSeriesQuery,
  newCustomersQuery,
  topProductsQuery,
  topCustomersQuery,
  tone,
}: {
  metricId: AnalyticsMetricId;
  periodLabel: string;
  granularity: AnalyticsGranularity;
  isSeries: boolean;
  salesSeriesQuery: ReturnType<typeof useAdminAnalyticsSalesQuery>;
  newCustomersQuery: ReturnType<typeof useAdminAnalyticsNewCustomersQuery>;
  topProductsQuery: ReturnType<typeof useAdminAnalyticsTopProductsQuery>;
  topCustomersQuery: ReturnType<typeof useAdminAnalyticsTopCustomersQuery>;
  tone: (typeof METRIC_TONES)[AnalyticsMetricId];
}) {
  if (isSeries) {
    const query = metricId === "newCustomers" ? newCustomersQuery : salesSeriesQuery;
    if (query.isPending) {
      return (
        <div className="px-4 py-4 md:px-5">
          <HeadlineSkeleton />
          <Skeleton className="h-48 w-full md:h-56" />
        </div>
      );
    }
    if (query.isError) {
      return <ErrorState onRetry={() => void query.refetch()} />;
    }

    if (metricId === "newCustomers") {
      const data = newCustomersQuery.data;
      const points: SeriesPoint[] = (data?.buckets ?? []).map((bucket) => ({
        id: bucket.periodStart,
        label: bucket.label,
        value: bucket.count,
      }));
      if (points.length === 0 || data?.total === 0) {
        return <EmptyState />;
      }
      return (
        <AnalyticsResult
          headline={String(data?.total ?? 0)}
          caption={`Clientes nuevos · ${periodLabel}`}
        >
          <TrendChart
            points={points}
            stroke={tone.stroke}
            mode="bars"
            formatValue={String}
          />
        </AnalyticsResult>
      );
    }

    const isSales = metricId === "sales";
    const points: SeriesPoint[] = (salesSeriesQuery.data?.buckets ?? []).map(
      (bucket) => ({
        id: bucket.periodStart,
        label: bucket.label,
        value: isSales ? moneyValue(bucket.total) : bucket.orderCount,
      }),
    );
    const total = points.reduce((sum, point) => sum + point.value, 0);
    if (points.length === 0 || total === 0) {
      return <EmptyState />;
    }
    const kpis = isSales
      ? analyticsSalesKpis(points, granularity)
      : null;
    return (
      <AnalyticsResult
        headline={isSales ? analyticsMoney(total) : String(total)}
        caption={`${isSales ? "Ventas" : "Pedidos"} · ${periodLabel}`}
      >
        <TrendChart
          points={points}
          stroke={tone.stroke}
          mode={isSales ? "area" : "bars"}
          formatValue={isSales ? analyticsMoney : String}
        />
        {kpis ? (
          <div className="grid gap-2 sm:grid-cols-3">
            <SalesKpiTile
              label={kpis.best.label}
              detail={kpis.best.detail}
              value={analyticsMoney(kpis.best.value)}
            />
            <SalesKpiTile
              label={kpis.worst.label}
              detail={kpis.worst.detail}
              value={analyticsMoney(kpis.worst.value)}
            />
            <SalesKpiTile
              label={kpis.averageLabel}
              value={analyticsMoney(kpis.averageValue)}
            />
          </div>
        ) : null}
      </AnalyticsResult>
    );
  }

  if (metricId === "topCustomers") {
    if (topCustomersQuery.isPending) {
      return <RankingSkeleton />;
    }
    if (topCustomersQuery.isError) {
      return <ErrorState onRetry={() => void topCustomersQuery.refetch()} />;
    }
    const items = topCustomersQuery.data?.items ?? [];
    if (items.length === 0) {
      return <EmptyState />;
    }
    const rows = items.map((item) => ({
      id: item.customerId,
      name: item.customerName,
      value: moneyValue(item.total),
    }));
    const total = rows.reduce((sum, row) => sum + row.value, 0);
    return (
      <AnalyticsResult
        headline={analyticsMoney(total)}
        caption={`Compras · ${periodLabel}`}
      >
        <RankingList
          rows={rows}
          tone={tone}
          formatValue={analyticsMoney}
        />
      </AnalyticsResult>
    );
  }

  if (topProductsQuery.isPending) {
    return <RankingSkeleton />;
  }
  if (topProductsQuery.isError) {
    return <ErrorState onRetry={() => void topProductsQuery.refetch()} />;
  }
  const items = topProductsQuery.data?.items ?? [];
  if (items.length === 0) {
    return <EmptyState />;
  }
  const rows = items.map((item) => ({
    id: item.productId,
    name: item.productName,
    value: item.quantity,
  }));
  const total = rows.reduce((sum, row) => sum + row.value, 0);
  return (
    <AnalyticsResult
      headline={String(total)}
      caption={`Top 5 · ${periodLabel}`}
    >
      <RankingList rows={rows} tone={tone} formatValue={String} />
    </AnalyticsResult>
  );
}

function RankingSkeleton() {
  return (
    <div className="px-4 py-4 md:px-5">
      <HeadlineSkeleton />
      <Skeleton className="h-48 w-full md:h-56" />
    </div>
  );
}

function SalesKpiTile({
  label,
  detail,
  value,
}: {
  label: string;
  detail?: string;
  value: string;
}) {
  return (
    <div className="rounded-lg bg-sf-bg px-3 py-2.5">
      <p className="text-xs font-semibold text-sf-muted">{label}</p>
      <p className="mt-0.5 text-sm font-bold tabular-nums text-sf-ink">
        {value}
      </p>
      {detail ? (
        <p className="mt-0.5 text-xs text-sf-muted">{detail}</p>
      ) : null}
    </div>
  );
}

function AnalyticsResult({
  headline,
  caption,
  children,
}: {
  headline: string;
  caption: string;
  children: ReactNode;
}) {
  return (
    <div className="px-4 py-4 md:px-5">
      <div className="mb-3 flex items-end justify-between gap-3">
        {/* div (no <p>): puede contener elementos de bloque durante la carga */}
        <div className="text-xl font-bold tracking-tight tabular-nums text-sf-ink">
          {headline}
        </div>
        <div className="shrink-0 text-xs text-sf-muted">{caption}</div>
      </div>
      {children}
    </div>
  );
}

function HeadlineSkeleton() {
  return (
    <div className="mb-3 flex items-end justify-between gap-3">
      <Skeleton className="h-7 w-32" />
      <Skeleton className="h-4 w-24" />
    </div>
  );
}

/** Área de visualización con altura estable: reserva el mismo alto que ocupa
 *  la gráfica (h-48/md:h-56) para que loading/success/empty/error no muevan
 *  la tarjeta. Centra el contenido del estado. */
function AnalyticsContentFrame({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-48 items-center justify-center px-4 py-4 md:min-h-56 md:px-5">
      {children}
    </div>
  );
}

function EmptyState({ message = "No hay datos para este período." }: { message?: string }) {
  return (
    <AnalyticsContentFrame>
      <p className="text-center text-sm text-sf-muted">{message}</p>
    </AnalyticsContentFrame>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <AnalyticsContentFrame>
      <div className="flex flex-col items-center gap-3">
        <p className="text-center text-sm text-sf-muted">
          No pudimos cargar estos datos.
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex min-h-8 items-center gap-1.5 rounded-lg border border-sf-border bg-sf-surface px-3 py-1.5 text-sm font-semibold text-sf-primary transition-colors hover:bg-sf-bg"
        >
          <RefreshIcon className="h-4 w-4" />
          Reintentar
        </button>
      </div>
    </AnalyticsContentFrame>
  );
}

function TrendChart({
  points,
  stroke,
  mode,
  formatValue,
}: {
  points: SeriesPoint[];
  stroke: string;
  mode: "area" | "bars";
  formatValue: (value: number) => string;
}) {
  const width = 960;
  const height = 240;
  const padX = 36;
  const padY = 24;
  const chartW = width - padX * 2;
  const chartH = height - padY * 2 - 28;
  const maxValue = Math.max(...points.map((point) => point.value), 0);

  const labelStep = Math.max(1, Math.ceil(points.length / 10));

  return (
    <div className="w-full">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="h-48 w-full md:h-56"
        role="img"
        aria-label="Evolución del período"
      >
        <defs>
          <linearGradient id="sfAnalyticsFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={stroke} stopOpacity="0.18" />
            <stop offset="100%" stopColor={stroke} stopOpacity="0.02" />
          </linearGradient>
        </defs>
        {[0.25, 0.5, 0.75, 1].map((ratio) => {
          const y = padY + chartH * (1 - ratio);
          return (
            <line
              key={ratio}
              x1={padX}
              x2={width - padX}
              y1={y}
              y2={y}
              stroke="#e7eaf0"
              strokeDasharray="4 6"
            />
          );
        })}
        {mode === "bars" ? (
          points.map((point, index) => {
            const slot = chartW / points.length;
            const barW = Math.max(slot * 0.6, 4);
            const x = padX + slot * index + (slot - barW) / 2;
            const barH = maxValue > 0 ? (point.value / maxValue) * chartH : 0;
            return (
              <rect
                key={point.id}
                x={x}
                y={padY + chartH - barH}
                width={barW}
                height={barH}
                rx={Math.min(4, barW / 2)}
                fill={stroke}
                opacity="0.85"
              >
                <title>
                  {point.label}: {formatValue(point.value)}
                </title>
              </rect>
            );
          })
        ) : (
          <>
            {(() => {
              const linePoints = points.map((point, index) => ({
                x:
                  points.length === 1
                    ? padX + chartW / 2
                    : padX + (index / Math.max(points.length - 1, 1)) * chartW,
                y: padY + chartH - (maxValue > 0 ? (point.value / maxValue) * chartH : 0),
                point,
              }));
              const linePath = linePoints
                .map((p, index) => `${index === 0 ? "M" : "L"} ${p.x} ${p.y}`)
                .join(" ");
              const areaPath =
                linePoints.length === 0
                  ? ""
                  : `${linePath} L ${linePoints[linePoints.length - 1]!.x} ${padY + chartH} L ${linePoints[0]!.x} ${padY + chartH} Z`;
              return (
                <>
                  <path d={areaPath} fill="url(#sfAnalyticsFill)" />
                  <path
                    d={linePath}
                    fill="none"
                    stroke={stroke}
                    strokeWidth="2.75"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  {linePoints.map((p) => (
                    <circle
                      key={p.point.id}
                      cx={p.x}
                      cy={p.y}
                      r="4"
                      fill="#ffffff"
                      stroke={stroke}
                      strokeWidth="2"
                    >
                      <title>
                        {p.point.label}: {formatValue(p.point.value)}
                      </title>
                    </circle>
                  ))}
                </>
              );
            })()}
          </>
        )}
        {points.map((point, index) =>
          index % labelStep === 0 || index === points.length - 1 ? (
            <text
              key={`${point.id}-label`}
              x={
                mode === "bars"
                  ? padX + (chartW / points.length) * index + chartW / points.length / 2
                  : points.length === 1
                    ? padX + chartW / 2
                    : padX + (index / Math.max(points.length - 1, 1)) * chartW
              }
              y={height - 10}
              textAnchor="middle"
              fill="#667085"
              fontSize="13"
            >
              {point.label}
            </text>
          ) : null,
        )}
      </svg>
    </div>
  );
}

function RankingList({
  rows,
  tone,
  formatValue,
}: {
  rows: { id: string; name: string; value: number }[];
  tone: { bar: string };
  formatValue: (value: number) => string;
}) {
  const maxValue = Math.max(...rows.map((row) => row.value), 0);
  return (
    <div className="divide-y divide-sf-border/80">
      {rows.map((row, index) => (
        <div key={row.id} className="flex items-center gap-3 px-4 py-2.5 md:px-5">
          <span className="w-5 shrink-0 text-right text-xs font-bold tabular-nums text-sf-muted">
            {index + 1}
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex items-baseline justify-between gap-3">
              <span className="truncate text-sm font-semibold text-sf-ink">
                {row.name}
              </span>
              <span className="shrink-0 text-sm font-bold tabular-nums text-sf-ink">
                {formatValue(row.value)}
              </span>
            </div>
            <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-sf-bg">
              <div
                className={cx("h-full rounded-full", tone.bar)}
                style={{
                  width: maxValue > 0 ? `${(row.value / maxValue) * 100}%` : "0%",
                }}
              />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
