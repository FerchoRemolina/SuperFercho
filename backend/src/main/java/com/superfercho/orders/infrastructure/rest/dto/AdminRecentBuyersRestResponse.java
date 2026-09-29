package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminRecentBuyerResult;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminRecentBuyersRestResponse(List<AdminRecentBuyerRestResponse> items) {

    public static AdminRecentBuyersRestResponse from(List<AdminRecentBuyerResult> buyers) {
        return new AdminRecentBuyersRestResponse(
                buyers.stream().map(AdminRecentBuyerRestResponse::from).toList());
    }

    public record AdminRecentBuyerRestResponse(
            UUID customerId,
            String displayName,
            Instant lastOrderAt,
            long orderCount,
            Money lastOrderTotal) {

        public static AdminRecentBuyerRestResponse from(AdminRecentBuyerResult buyer) {
            return new AdminRecentBuyerRestResponse(
                    buyer.customerId(),
                    buyer.displayName(),
                    buyer.lastOrderAt(),
                    buyer.orderCount(),
                    buyer.lastOrderTotal());
        }
    }
}
