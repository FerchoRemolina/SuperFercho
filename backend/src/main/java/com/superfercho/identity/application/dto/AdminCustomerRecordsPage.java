package com.superfercho.identity.application.dto;

import java.util.List;

public record AdminCustomerRecordsPage(
        List<AdminCustomerRecordListItem> items, int page, int size, long totalElements) {

    public AdminCustomerRecordsPage {
        items = List.copyOf(items);
    }
}
