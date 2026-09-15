package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PagedResult;
import java.util.List;

public record PagedOrdersRestResponse(List<OrderRestResponse> items, int page, int size, long totalElements) {

    public static PagedOrdersRestResponse from(PagedResult<OrderResult> page) {
        return new PagedOrdersRestResponse(
                page.items().stream().map(OrderRestResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
