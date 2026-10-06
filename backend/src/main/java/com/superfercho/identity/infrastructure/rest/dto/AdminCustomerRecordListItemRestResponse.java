package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCustomerRecordListItemRestResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String billingFirstName,
        String billingLastName,
        String email,
        String phone,
        String status,
        long accountCount,
        long activeAccounts,
        long inactiveAccounts,
        long deletedAccounts,
        long orderCount,
        Money totalSpent,
        Instant lastOrderAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminCustomerRecordListItemRestResponse from(AdminCustomerRecordListItem item) {
        return new AdminCustomerRecordListItemRestResponse(
                item.id(),
                item.documentType(),
                item.documentNumber(),
                item.firstName(),
                item.lastName(),
                item.email(),
                item.phone(),
                item.status().name(),
                item.accountCount(),
                item.activeAccounts(),
                item.inactiveAccounts(),
                item.deletedAccounts(),
                item.orderCount(),
                item.totalSpent(),
                item.lastOrderAt(),
                item.createdAt(),
                item.updatedAt());
    }
}
