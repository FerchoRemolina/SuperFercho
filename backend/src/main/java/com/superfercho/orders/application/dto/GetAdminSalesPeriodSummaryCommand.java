package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.model.SalesPeriodGranularity;
import java.util.Locale;

public record GetAdminSalesPeriodSummaryCommand(SalesPeriodGranularity granularity) {

    public static GetAdminSalesPeriodSummaryCommand of(String raw) {
        if (raw == null || raw.isBlank()) {
            return new GetAdminSalesPeriodSummaryCommand(SalesPeriodGranularity.WEEK);
        }
        try {
            return new GetAdminSalesPeriodSummaryCommand(
                    SalesPeriodGranularity.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrderException("Unsupported sales granularity: " + raw);
        }
    }
}
