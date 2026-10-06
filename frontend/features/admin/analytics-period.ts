/**
 * MODELO DE PERÍODOS DE ANALÍTICA (contrato backend real).
 *
 * El backend expone ventanas [from, to) arbitrarias:
 *  - GET /admin/orders/dashboard/sales?from&to&granularity
 *  - GET /admin/customers/dashboard/new?from&to&granularity
 * con from/to como instantes ISO-8601 (fechas sueltas se interpretan en
 * America/Bogota) y granularidad de bucket HOUR|DAY|MONTH (HOUR ≤ 7 días,
 * DAY ≤ 400 días, MONTH ≤ 5 años).
 *
 * Este módulo centraliza selección → [from, to) + granularidad + label +
 * cacheKey para que admin-analytics-card.tsx no tenga lógica de fechas.
 * Todos los límites de calendario se calculan en America/Bogota (UTC−5 fijo,
 * sin DST) y se envían como instantes ISO-8601 inequívocos.
 */

export type AnalyticsPresetId = "today" | "thisWeek" | "thisMonth" | "thisYear";

export type AnalyticsPeriodSelection =
  | { kind: "preset"; id: AnalyticsPresetId }
  /** Fechas calendario YYYY-MM-DD (Bogotá), ambas inclusivas. */
  | { kind: "custom"; from: string; to: string };

export type AnalyticsGranularity = "HOUR" | "DAY" | "MONTH";

export type ResolvedAnalyticsPeriod = {
  /** Instante ISO-8601 inclusivo. */
  from: string;
  /** Instante ISO-8601 exclusivo. */
  to: string;
  /** Granularidad de bucket soportada por el backend para el rango. */
  granularity: AnalyticsGranularity;
  label: string;
  /** Estable e incluye rango/granularidad: cada período consultado usa una
   *  entrada de caché propia. */
  cacheKey: string;
};

const ANALYTICS_HOUR_MS = 3_600_000;
const ANALYTICS_DAY_MS = 86_400_000;
/** Colombia: UTC−5 fijo, sin horario de verano. */
const BOGOTA_OFFSET_MS = 5 * ANALYTICS_HOUR_MS;

/** Medianoche Bogotá del día "YYYY-MM-DD", expresada en ms UTC. */
function bogotaDayStartUtcMs(dateStr: string): number {
  const [y, m, d] = dateStr.split("-").map(Number);
  return Date.UTC(y, m - 1, d, 5, 0, 0);
}

/** Partes de fecha (YYYY, M, D) de un instante según calendario Bogotá. */
function bogotaParts(instantMs: number): { y: number; m: number; d: number } {
  // Bogotá = UTC−5 → la hora local se obtiene restando el offset.
  const shifted = new Date(instantMs - BOGOTA_OFFSET_MS);
  return {
    y: shifted.getUTCFullYear(),
    m: shifted.getUTCMonth(),
    d: shifted.getUTCDate(),
  };
}

function bogotaWeekday(day0UtcMs: number): number {
  return new Date(day0UtcMs).getUTCDay();
}

function iso(ms: number): string {
  return new Date(ms).toISOString();
}

function isLeapYear(y: number): boolean {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
}

function daysInMonth(y: number, m: number): number {
  return [31, isLeapYear(y) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m]!;
}

export function isValidAnalyticsDateInput(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  const [y, m, d] = value.split("-").map(Number);
  return (
    m >= 1 && m <= 12 && d >= 1 && d <= daysInMonth(y, m - 1) && y >= 2000 && y <= 2200
  );
}

const MONTHS_SHORT_ES = [
  "ene", "feb", "mar", "abr", "may", "jun",
  "jul", "ago", "sep", "oct", "nov", "dic",
] as const;

/** Etiqueta compacta "10 sep → 30 sep" a partir de fechas YYYY-MM-DD
 *  (sin pasar por Date: inmutable frente a zonas horarias). */
export function analyticsShortRangeLabel(from: string, to: string): string {
  const part = (dateStr: string, withYear: boolean) => {
    const [y, m, d] = dateStr.split("-");
    return `${Number(d)} ${MONTHS_SHORT_ES[Number(m) - 1]}${withYear ? ` ${y}` : ""}`;
  };
  const sameYear = from.slice(0, 4) === to.slice(0, 4);
  if (from === to) {
    return part(from, !sameYear);
  }
  const withYear = !sameYear;
  return `${part(from, withYear)} → ${part(to, withYear)}`;
}

/** Granularidad de bucket según el tamaño del rango, dentro de los límites
 *  del backend (HOUR ≤ 7 días, DAY ≤ 400 días, MONTH ≤ 5 años). */
export function analyticsGranularityForRange(
  fromMs: number,
  toMs: number,
): AnalyticsGranularity {
  const days = (toMs - fromMs) / ANALYTICS_DAY_MS;
  if (days <= 2) {
    return "HOUR";
  }
  if (days <= 62) {
    return "DAY";
  }
  return "MONTH";
}

function resolvePreset(
  id: AnalyticsPresetId,
  now: Date,
): { fromMs: number; toMs: number; granularity: AnalyticsGranularity } {
  const { y, m, d } = bogotaParts(now.getTime());
  const day0 = Date.UTC(y, m, d, 5);
  const endOfToday = day0 + ANALYTICS_DAY_MS;
  const tomorrow0 = day0 + ANALYTICS_DAY_MS;
  let fromMs: number;
  let calendarEnd: number;
  switch (id) {
    case "today":
      fromMs = day0;
      calendarEnd = tomorrow0;
      break;
    case "thisWeek": {
      // Semana calendario con inicio lunes (Colombia).
      const monday = day0 - ((bogotaWeekday(day0) + 6) % 7) * ANALYTICS_DAY_MS;
      fromMs = monday;
      calendarEnd = monday + 7 * ANALYTICS_DAY_MS;
      break;
    }
    case "thisMonth":
      fromMs = Date.UTC(y, m, 1, 5);
      calendarEnd = Date.UTC(y, m + 1, 1, 5);
      break;
    case "thisYear":
      fromMs = Date.UTC(y, 0, 1, 5);
      calendarEnd = Date.UTC(y + 1, 0, 1, 5);
      break;
  }
  // Los presets representan tiempo transcurrido: inicio del período → AHORA.
  // El backend rellena buckets hasta `to`, así que acotamos `to` al presente
  // para no generar buckets futuros (días/horas que aún no existen).
  let toMs = Math.min(calendarEnd, endOfToday);
  const granularity = analyticsGranularityForRange(fromMs, toMs);
  if (granularity === "HOUR") {
    // Cuantizado al fin de la hora en curso: eje compacto sin horas futuras.
    const currentHourEnd =
      Math.floor(now.getTime() / ANALYTICS_HOUR_MS) * ANALYTICS_HOUR_MS +
      ANALYTICS_HOUR_MS;
    toMs = Math.min(toMs, currentHourEnd);
  }
  return { fromMs, toMs, granularity };
}

function resolveCustom(from: string, to: string): { fromMs: number; toMs: number } {
  if (!isValidAnalyticsDateInput(from) || !isValidAnalyticsDateInput(to)) {
    throw new Error(`Rango personalizado inválido: ${from} → ${to}`);
  }
  const fromMs = bogotaDayStartUtcMs(from);
  const toMs = bogotaDayStartUtcMs(to);
  if (fromMs > toMs) {
    throw new Error(`Rango personalizado desordenado: ${from} → ${to}`);
  }
  // "to" es una fecha calendario inclusiva → se extiende hasta su fin
  // (semántica [from, to): el día "to" completo queda dentro del rango).
  return { fromMs, toMs: toMs + ANALYTICS_DAY_MS };
}

/** Resuelve cualquier selección a [from, to) + granularidad + metadatos. */
export function resolveAnalyticsPeriod(
  selection: AnalyticsPeriodSelection,
  now: Date = new Date(),
): ResolvedAnalyticsPeriod {
  let fromMs: number;
  let toMs: number;
  let granularity: AnalyticsGranularity;
  if (selection.kind === "preset") {
    const preset = resolvePreset(selection.id, now);
    fromMs = preset.fromMs;
    toMs = preset.toMs;
    granularity = preset.granularity;
  } else {
    const custom = resolveCustom(selection.from, selection.to);
    fromMs = custom.fromMs;
    toMs = custom.toMs;
    granularity = analyticsGranularityForRange(fromMs, toMs);
  }
  const label =
    selection.kind === "preset"
      ? analyticsPresetLabel(selection.id)
      : analyticsShortRangeLabel(selection.from, selection.to);
  const rangeTag = `${iso(fromMs).slice(0, 10)}:${iso(toMs).slice(0, 10)}`;
  return {
    from: iso(fromMs),
    to: iso(toMs),
    granularity,
    label,
    cacheKey: `${selection.kind === "preset" ? selection.id : "custom"}:${rangeTag}:${granularity}`,
  };
}

export function analyticsPresetLabel(id: AnalyticsPresetId): string {
  switch (id) {
    case "today":
      return "Hoy";
    case "thisWeek":
      return "Esta semana";
    case "thisMonth":
      return "Este mes";
    case "thisYear":
      return "Este año";
  }
}

export type AnalyticsBucketKpi = {
  label: string;
  detail: string;
  value: number;
};

export type AnalyticsSalesKpis = {
  best: AnalyticsBucketKpi;
  worst: AnalyticsBucketKpi;
  averageLabel: string;
  averageValue: number;
};

/**
 * KPIs de ventas derivados client-side de buckets YA RELLENADOS por backend:
 * un bucket sin ventas permanece en 0 y participa en el promedio cuando
 * corresponde al período. KPIs de MONTO: mejor/menor por valor de venta,
 * no por cantidad de pedidos.
 */
export function analyticsSalesKpis(
  points: readonly { label: string; value: number }[],
  granularity: AnalyticsGranularity,
): AnalyticsSalesKpis | null {
  if (points.length === 0) {
    return null;
  }
  let best = points[0]!;
  let worst = points[0]!;
  let total = 0;
  for (const point of points) {
    if (point.value > best.value) {
      best = point;
    }
    if (point.value < worst.value) {
      worst = point;
    }
    total += point.value;
  }
  const unit = granularity === "HOUR" ? "hora" : granularity === "MONTH" ? "mes" : "día";
  const averageLabel = granularity === "HOUR" ? "Promedio por hora" : "Promedio diario";
  return {
    best: { label: `Mejor ${unit}`, detail: best.label, value: best.value },
    worst: { label: `Menor ${unit}`, detail: worst.label, value: worst.value },
    averageLabel,
    averageValue: total / points.length,
  };
}
