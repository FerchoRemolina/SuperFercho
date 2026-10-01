package com.superfercho.orders.application.dto;

import java.time.Instant;

/** Arbitrary [from, to) period for the admin orders dashboard summary. */
public record GetAdminDashboardSummaryCommand(Instant from, Instant to) {

    public GetAdminDashboardSummaryCommand {
        DashboardPeriods.requireValidRange(from, to);
    }

    public static GetAdminDashboardSummaryCommand of(String rawFrom, String rawTo) {
        return new GetAdminDashboardSummaryCommand(
                DashboardPeriods.parseBound(rawFrom, "from"),
                DashboardPeriods.parseBound(rawTo, "to"));
    }
}
