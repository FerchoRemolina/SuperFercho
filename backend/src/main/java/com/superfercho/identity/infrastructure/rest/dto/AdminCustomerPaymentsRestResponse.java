package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialPaymentView;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCustomerPaymentsRestResponse(
        List<AdminCustomerPaymentRestResponse> items, int page, int size, long totalElements) {

    public static AdminCustomerPaymentsRestResponse from(
            AdminPagedResult<CustomerCommercialPaymentView> page) {
        return new AdminCustomerPaymentsRestResponse(
                page.items().stream().map(AdminCustomerPaymentRestResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }

    public record AdminCustomerPaymentRestResponse(
            UUID id,
            UUID orderId,
            Money amount,
            String paymentMethod,
            String status,
            String providerReference,
            Instant createdAt,
            Instant updatedAt,
            Instant refundedAt) {

        static AdminCustomerPaymentRestResponse from(CustomerCommercialPaymentView payment) {
            return new AdminCustomerPaymentRestResponse(
                    payment.id(),
                    payment.orderId(),
                    payment.amount(),
                    payment.paymentMethod(),
                    payment.status(),
                    payment.providerReference(),
                    payment.createdAt(),
                    payment.updatedAt(),
                    payment.refundedAt());
        }
    }
}
