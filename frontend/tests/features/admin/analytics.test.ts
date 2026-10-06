import { describe, expect, it } from "vitest";
import {
  analyticsSalesKpis,
  analyticsShortRangeLabel,
  isValidAnalyticsDateInput,
  resolveAnalyticsPeriod,
} from "@/features/admin/analytics-period";
import { mergeAdminProductsFilters } from "@/features/admin/presentation";

describe("resolveAnalyticsPeriod (presets, calendario Bogotá UTC−5)", () => {
  it("Hoy termina al fin de la hora en curso (sin horas futuras)", () => {
    const now = new Date("2026-09-30T14:27:43.000Z");
    const resolved = resolveAnalyticsPeriod({ kind: "preset", id: "today" }, now);
    expect(resolved.from).toBe("2026-09-30T05:00:00.000Z");
    expect(resolved.to).toBe("2026-09-30T15:00:00.000Z");
    expect(resolved.granularity).toBe("HOUR");
    expect(resolved.label).toBe("Hoy");
  });

  it("Hoy respeta el día Bogotá aunque UTC ya cambió de fecha", () => {
    // 03:00 UTC = 22:00 del 30/09 en Bogotá → "hoy" es el 30/09 y el rango
    // termina al fin de la hora Bogotá en curso (23:00), sin cruzar de día.
    const now = new Date("2026-10-01T03:00:00.000Z");
    const resolved = resolveAnalyticsPeriod({ kind: "preset", id: "today" }, now);
    expect(resolved.from).toBe("2026-09-30T05:00:00.000Z");
    expect(resolved.to).toBe("2026-10-01T04:00:00.000Z");
  });

  it("Esta semana = lunes de la semana en curso → mañana (≠ últimos 7 días)", () => {
    // 2026-09-30 es miércoles; la semana arrancó el lunes 28/09.
    const now = new Date("2026-09-30T14:27:43.000Z");
    const resolved = resolveAnalyticsPeriod({ kind: "preset", id: "thisWeek" }, now);
    expect(resolved.from).toBe("2026-09-28T05:00:00.000Z");
    expect(resolved.to).toBe("2026-10-01T05:00:00.000Z");
    expect(resolved.granularity).toBe("DAY");
    expect(resolved.label).toBe("Esta semana");
  });

  it("Este mes = 01/09 00:00 → fin del día en curso (nunca fin de mes futuro)", () => {
    const now = new Date("2026-09-30T14:27:43.000Z");
    const resolved = resolveAnalyticsPeriod({ kind: "preset", id: "thisMonth" }, now);
    expect(resolved.from).toBe("2026-09-01T05:00:00.000Z");
    expect(resolved.to).toBe("2026-10-01T05:00:00.000Z");
    expect(resolved.granularity).toBe("DAY");
  });

  it("Este mes en el día 1: rango compacto hasta la hora actual, sin días futuros", () => {
    // Hoy = 01/10/2026 09:27 Bogotá → solo el día 1, hasta la hora en curso.
    const now = new Date("2026-10-01T14:27:43.000Z");
    const resolved = resolveAnalyticsPeriod({ kind: "preset", id: "thisMonth" }, now);
    expect(resolved.from).toBe("2026-10-01T05:00:00.000Z");
    expect(resolved.to).toBe("2026-10-01T15:00:00.000Z");
    expect(resolved.granularity).toBe("HOUR");
  });

  it("Este año = 01/01 → ahora, con granularidad MONTH (sin meses futuros)", () => {
    const now = new Date("2026-09-30T14:27:43.000Z");
    const resolved = resolveAnalyticsPeriod({ kind: "preset", id: "thisYear" }, now);
    expect(resolved.from).toBe("2026-01-01T05:00:00.000Z");
    expect(resolved.to).toBe("2026-10-01T05:00:00.000Z");
    expect(resolved.granularity).toBe("MONTH");
  });

  it("los presets comparten [from,to] pero cada uno tiene cacheKey propia", () => {
    const now = new Date("2026-09-30T14:27:43.000Z");
    const week = resolveAnalyticsPeriod({ kind: "preset", id: "thisWeek" }, now);
    const month = resolveAnalyticsPeriod({ kind: "preset", id: "thisMonth" }, now);
    expect(week.cacheKey).not.toBe(month.cacheKey);
    expect(week.cacheKey).toBe("thisWeek:2026-09-28:2026-10-01:DAY");
    expect(month.cacheKey).toBe("thisMonth:2026-09-01:2026-10-01:DAY");
  });
});

describe("resolveAnalyticsPeriod (personalizado)", () => {
  it("fechas inclusivas: 10/05 → 31/05 cubre el día 31 completo ([from, to))", () => {
    const resolved = resolveAnalyticsPeriod({
      kind: "custom",
      from: "2026-05-01",
      to: "2026-05-31",
    });
    expect(resolved.from).toBe("2026-05-01T05:00:00.000Z");
    expect(resolved.to).toBe("2026-06-01T05:00:00.000Z");
    expect(resolved.granularity).toBe("DAY");
  });

  it("el botón muestra el rango compacto y la cacheKey serializa el rango real consultado", () => {
    const resolved = resolveAnalyticsPeriod({
      kind: "custom",
      from: "2026-09-10",
      to: "2026-09-20",
    });
    expect(resolved.label).toBe("10 sep → 20 sep");
    // La clave refleja el [from, to) real: to extendido al día siguiente.
    expect(resolved.cacheKey).toBe("custom:2026-09-10:2026-09-21:DAY");
  });

  it("rechaza rangos desordenados o fechas inválidas", () => {
    expect(() =>
      resolveAnalyticsPeriod({ kind: "custom", from: "2026-05-15", to: "2026-05-10" }),
    ).toThrow();
    expect(() =>
      resolveAnalyticsPeriod({ kind: "custom", from: "2026-02-30", to: "2026-03-01" }),
    ).toThrow();
    expect(isValidAnalyticsDateInput("2026-02-29")).toBe(false);
    expect(isValidAnalyticsDateInput("2028-02-29")).toBe(true);
  });

  it("rangos largos proponen granularidad MONTH dentro de los límites del backend", () => {
    const resolved = resolveAnalyticsPeriod({
      kind: "custom",
      from: "2025-01-01",
      to: "2025-12-31",
    });
    expect(resolved.granularity).toBe("MONTH");
  });

  it("rangos cortos proponen granularidad HOUR (≤ 7 días permitidos por backend)", () => {
    const resolved = resolveAnalyticsPeriod({
      kind: "custom",
      from: "2026-05-10",
      to: "2026-05-11",
    });
    expect(resolved.granularity).toBe("HOUR");
  });
});

describe("analyticsShortRangeLabel", () => {
  it("formato compacto dd mmm dentro del mismo año", () => {
    expect(analyticsShortRangeLabel("2026-09-10", "2026-09-30")).toBe(
      "10 sep → 30 sep",
    );
  });

  it("rango de un solo día no repite la fecha", () => {
    expect(analyticsShortRangeLabel("2026-09-10", "2026-09-10")).toBe("10 sep");
  });

  it("incluye el año cuando el rango cruza de año", () => {
    expect(analyticsShortRangeLabel("2025-12-31", "2026-01-05")).toBe(
      "31 dic 2025 → 5 ene 2026",
    );
  });
});

describe("mergeAdminProductsFilters (filtro Categoria - Todas)", () => {
  it("Todas (null) elimina completamente el filtro de categoria", () => {
    const merged = mergeAdminProductsFilters(
      { text: "", categoryId: "cat-1", status: undefined },
      { categoryId: null },
    );
    expect(merged.categoryId).toBeUndefined();
  });

  it("un cambio sin categoria mantiene el filtro actual", () => {
    const merged = mergeAdminProductsFilters(
      { text: "cafe", categoryId: "cat-1", status: undefined },
      { text: "te" },
    );
    expect(merged.categoryId).toBe("cat-1");
    expect(merged.text).toBe("te");
  });

  it("aplica categoria nueva y limpia Estado con Todos", () => {
    const merged = mergeAdminProductsFilters(
      { text: "", categoryId: undefined, status: "ACTIVE" },
      { categoryId: "cat-2", status: "" },
    );
    expect(merged.categoryId).toBe("cat-2");
    expect(merged.status).toBeUndefined();
  });
});

describe("analyticsSalesKpis (monto de ventas por bucket)", () => {
  const points = [
    { label: "25 may", value: 0 },
    { label: "26 may", value: 85 },
    { label: "27 may", value: 35 },
  ];

  it("deriva mejor día, menor día y promedio incluyendo buckets en cero", () => {
    const kpis = analyticsSalesKpis(points, "DAY");
    expect(kpis).not.toBeNull();
    expect(kpis?.best).toEqual({
      label: "Mejor día",
      detail: "26 may",
      value: 85,
    });
    expect(kpis?.worst).toEqual({
      label: "Menor día",
      detail: "25 may",
      value: 0,
    });
    expect(kpis?.averageValue).toBeCloseTo(40, 5);
    expect(kpis?.averageLabel).toBe("Promedio diario");
  });

  it("usa etiquetas por granularidad (hora/mes) y calcula promedio", () => {
    const hourly = analyticsSalesKpis(
      [
        { label: "08:00", value: 10 },
        { label: "09:00", value: 30 },
      ],
      "HOUR",
    );
    expect(hourly?.best.label).toBe("Mejor hora");
    expect(hourly?.worst.label).toBe("Menor hora");
    expect(hourly?.averageLabel).toBe("Promedio por hora");
    expect(hourly?.averageValue).toBe(20);

    const monthly = analyticsSalesKpis(
      [{ label: "ene 2026", value: 900 }],
      "MONTH",
    );
    expect(monthly?.best.label).toBe("Mejor mes");
    expect(monthly?.worst.label).toBe("Menor mes");
    expect(monthly?.averageValue).toBe(900);
  });

  it("devuelve null sin buckets", () => {
    expect(analyticsSalesKpis([], "DAY")).toBeNull();
  });
});
