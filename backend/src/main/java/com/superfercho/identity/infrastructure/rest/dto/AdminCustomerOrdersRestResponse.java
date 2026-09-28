package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialOrderView;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCustomerOrdersRestResponse(
        List<AdminCustomerOrderRestResponse> items, int page, int size, long totalElements) {

    public static AdminCustomerOrdersRestResponse from(AdminPagedResult<CustomerCommercialOrderView> page) {
        return new AdminCustomerOrdersRestResponse(
                page.items().stream().map(AdminCustomerOrderRestResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }

    public record AdminCustomerOrderRestResponse(
            UUID id,
            String orderNumber,
            UUID customerId,
            String status,
            Money subtotal,
            Money total,
            UUID paymentId,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant updatedAt) {

        static AdminCustomerOrderRestResponse from(CustomerCommercialOrderView order) {
            return new AdminCustomerOrderRestResponse(
                    order.id(),
                    order.orderNumber(),
                    order.customerId(),
                    order.status(),
                    order.subtotal(),
                    order.total(),
                    order.paymentId(),
                    order.createdAt(),
                    order.confirmedAt(),
                    order.cancelledAt(),
                    order.updatedAt());
        }
    }
}
