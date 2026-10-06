package com.superfercho.identity.infrastructure.rest.dto;

import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import java.util.List;

public record AdminCustomerRecordsPageRestResponse(
        List<AdminCustomerRecordListItemRestResponse> items, int page, int size, long totalElements) {

    public static AdminCustomerRecordsPageRestResponse from(AdminCustomerRecordsPage page) {
        return new AdminCustomerRecordsPageRestResponse(
                page.items().stream().map(AdminCustomerRecordListItemRestResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
