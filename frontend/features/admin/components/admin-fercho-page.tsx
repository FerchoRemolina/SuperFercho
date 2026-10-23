"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAdminFerchoAlerts,
  getAdminFerchoRestrictions,
  getAdminFerchoSummary,
  liftAdminFerchoRestriction,
  resolveAdminFerchoAlert,
  type AdminFerchoAlert,
} from "@/features/admin/api";
import { isApiError } from "@/shared/errors/api-problem";
import { Button } from "@/shared/ui/button";
import { Container } from "@/shared/ui/container";

/** Costo mostrado solo cuando el backend lo calcula; nunca se inventa. */
function costLabel(estimatedCost: string | null): string {
  return estimatedCost ?? "No disponible";
}

export function AdminFerchoPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string | undefined>("OPEN");
  const [error, setError] = useState<string | null>(null);
  const [working, setWorking] = useState<string | null>(null);

  const summaryQuery = useQuery({
    queryKey: ["admin", "fercho", "summary"],
    queryFn: () => getAdminFerchoSummary(24),
  });
  const alertsQuery = useQuery({
    queryKey: ["admin", "fercho", "alerts", statusFilter ?? "all"],
    queryFn: () => getAdminFerchoAlerts(statusFilter),
  });
  const restrictionsQuery = useQuery({
    queryKey: ["admin", "fercho", "restrictions", "active"],
    queryFn: () => getAdminFerchoRestrictions(true),
  });

  async function decide(alert: AdminFerchoAlert, action: "DISMISS" | "WARN" | "SUSPEND") {
    setError(null);
    setWorking(alert.id + action);
    try {
      await resolveAdminFerchoAlert(
        alert.id,
        action,
        `Decisión administrativa: ${action}`,
        action === "SUSPEND" ? 24 : undefined,
      );
      await queryClient.invalidateQueries({ queryKey: ["admin", "fercho"] });
    } catch (caught) {
      setError(
        isApiError(caught)
          ? caught.problem.detail ?? "No se pudo registrar la decisión."
          : "No se pudo registrar la decisión.",
      );
    } finally {
      setWorking(null);
    }
  }

  async function lift(restrictionId: string) {
    setError(null);
    setWorking(restrictionId + "lift");
    try {
      await liftAdminFerchoRestriction(restrictionId, "Levantada por administración");
      await queryClient.invalidateQueries({ queryKey: ["admin", "fercho"] });
    } catch (caught) {
      setError(
        isApiError(caught)
          ? caught.problem.detail ?? "No se pudo levantar la restricción."
          : "No se pudo levantar la restricción.",
      );
    } finally {
      setWorking(null);
    }
  }

  const summary = summaryQuery.data;

  return (
    <Container as="main" className="py-8 md:py-12">
      <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2.25rem]">
        Supervisión de Fercho
      </h1>

      {error ? (
        <div className="mt-4">
          <p className="text-sm font-semibold text-sf-error">{error}</p>
        </div>
      ) : null}

      <section aria-label="Uso de las últimas 24 horas" className="mt-6">
        <h2 className="text-base font-semibold text-sf-ink">
          Uso · últimas 24 horas
        </h2>
        {summaryQuery.isPending ? (
          <p className="mt-2 text-sm text-sf-muted">Cargando…</p>
        ) : summaryQuery.isError || summary === undefined ? (
          <p className="mt-2 text-sm text-sf-error">
            No se pudo cargar el uso de Fercho.
          </p>
        ) : (
          <dl className="mt-3 grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
            <div className="rounded-lg border border-sf-border bg-sf-surface p-3">
              <dt className="text-sf-muted">Solicitudes aceptadas</dt>
              <dd className="text-lg font-bold tabular-nums text-sf-ink">
                {summary.acceptedRequests}
              </dd>
            </div>
            <div className="rounded-lg border border-sf-border bg-sf-surface p-3">
              <dt className="text-sf-muted">Rechazadas por límite</dt>
              <dd className="text-lg font-bold tabular-nums text-sf-ink">
                {summary.rejectedRequests}
              </dd>
            </div>
            <div className="rounded-lg border border-sf-border bg-sf-surface p-3">
              <dt className="text-sf-muted">Llamadas al modelo</dt>
              <dd className="text-lg font-bold tabular-nums text-sf-ink">
                {summary.llmCalls}
              </dd>
            </div>
            <div className="rounded-lg border border-sf-border bg-sf-surface p-3">
              <dt className="text-sf-muted">
                Costo estimado (tokens{" "}
                {summary.promptTokens === null ? "n/d" : summary.promptTokens} +
                {summary.completionTokens === null ? "n/d" : summary.completionTokens})
              </dt>
              <dd className="text-lg font-bold tabular-nums text-sf-ink">
                {summary.estimatedCost ?? "No disponible"}
              </dd>
            </div>
          </dl>
        )}
        <p className="mt-2 text-xs text-sf-muted">
          Pausas activas: {summary?.activeAutoPauses ?? "—"}. Suspensiones
          activas: {summary?.activeSuspensions ?? "—"}. Alertas abiertas:{" "}
          {summary?.openAlerts ?? "—"}.
        </p>
      </section>

      <section aria-label="Alertas" className="mt-8">
        <h2 className="text-base font-semibold text-sf-ink">Alertas</h2>
        <div className="mt-2 flex gap-2">
          <Button
            type="button"
            variant={statusFilter === "OPEN" ? "primary" : "secondary"}
            onClick={() => setStatusFilter("OPEN")}
          >
            Abiertas
          </Button>
          <Button
            type="button"
            variant={statusFilter === undefined ? "primary" : "secondary"}
            onClick={() => setStatusFilter(undefined)}
          >
            Todas
          </Button>
        </div>
        {alertsQuery.isPending ? (
          <p className="mt-3 text-sm text-sf-muted">Cargando alertas…</p>
        ) : alertsQuery.data === undefined || alertsQuery.data.length === 0 ? (
          <p className="mt-3 text-sm text-sf-muted">Sin alertas.</p>
        ) : (
          <ul className="mt-3 grid gap-3">
            {alertsQuery.data.map((alert) => (
              <li
                key={alert.id}
                className="rounded-xl border border-sf-border bg-sf-surface p-4"
              >
                <p className="text-sm font-semibold text-sf-ink">
                  Cliente {alert.userId.slice(0, 8)}… · {alert.reason} ·{" "}
                  {alert.acceptedRequests} solicitudes · {alert.llmCalls}{" "}
                  llamadas
                </p>
                <p className="mt-1 text-xs text-sf-muted">
                  Estado: {alert.status} · Creada: {alert.createdAt}
                </p>
                {alert.status === "OPEN" ? (
                  <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                    <Button
                      type="button"
                      variant="secondary"
                      disabled={working !== null}
                      onClick={() => void decide(alert, "DISMISS")}
                    >
                      Descartar
                    </Button>
                    <Button
                      type="button"
                      variant="secondary"
                      disabled={working !== null}
                      onClick={() => void decide(alert, "WARN")}
                    >
                      Advertir
                    </Button>
                    <Button
                      type="button"
                      variant="primary"
                      disabled={working !== null}
                      onClick={() => void decide(alert, "SUSPEND")}
                    >
                      Suspender 24 h
                    </Button>
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-label="Restricciones activas" className="mt-8">
        <h2 className="text-base font-semibold text-sf-ink">
          Restricciones activas
        </h2>
        {restrictionsQuery.isPending ? (
          <p className="mt-2 text-sm text-sf-muted">Cargando…</p>
        ) : restrictionsQuery.data === undefined ||
          restrictionsQuery.data.length === 0 ? (
          <p className="mt-3 text-sm text-sf-muted">Sin restricciones activas.</p>
        ) : (
          <ul className="mt-3 grid gap-3">
            {restrictionsQuery.data.map((restriction) => (
              <li
                key={restriction.id}
                className="flex flex-col gap-2 rounded-xl border border-sf-border bg-sf-surface p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="text-sm font-semibold text-sf-ink">
                    {restriction.type} · cliente {restriction.userId.slice(0, 8)}…
                  </p>
                  <p className="text-xs text-sf-muted">
                    Vence: {restriction.expiresAt}
                  </p>
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={working !== null}
                  onClick={() => void lift(restriction.id)}
                >
                  Levantar
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </Container>
  );
}
