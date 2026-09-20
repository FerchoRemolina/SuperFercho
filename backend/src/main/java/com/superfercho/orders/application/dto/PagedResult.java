package com.superfercho.orders.application.dto;

import java.util.List;

public record PagedResult<T>(List<T> items, int page, int size, long totalElements) {

    public PagedResult {
        items = List.copyOf(items);
    }
}
