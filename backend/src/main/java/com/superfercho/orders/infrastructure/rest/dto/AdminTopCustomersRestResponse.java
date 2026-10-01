package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminCustomerSalesRow;
import com.superfercho.platform.money.Money;
import java.util.List;
import java.util.UUID;

/**
 * Top customers by purchased value for an arbitrary [from, to) period.
 * {@code customerName} is the shipping recipient name snapshot.
 */
public record AdminTopCustomersRestResponse(List<CustomerSalesRestResponse> items) {

    public static AdminTopCustomersRestResponse from(List<AdminCustomerSalesRow> rows) {
        return new AdminTopCustomersRestResponse(
                rows.stream().map(CustomerSalesRestResponse::from).toList());
    }

    public record CustomerSalesRestResponse(
            UUID customerId, String customerName, Money total, long orderCount) {

        public static CustomerSalesRestResponse from(AdminCustomerSalesRow row) {
            return new CustomerSalesRestResponse(
                    row.customerId(), row.customerName(), Money.cop(row.totalAmount()), row.orderCount());
        }
    }
}
