package com.superfercho.identity.application.dto;

import java.util.List;

public record AdminPagedResult<T>(List<T> items, int page, int size, long totalElements) {

    public AdminPagedResult {
        items = List.copyOf(items);
    }

    public static <T> AdminPagedResult<T> empty(int page, int size) {
        return new AdminPagedResult<>(List.of(), page, size, 0);
    }
}
