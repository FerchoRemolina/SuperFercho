package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminOrderListItemResult;
import com.superfercho.orders.application.dto.PagedResult;
import java.util.List;

public record PagedAdminOrdersRestResponse(
        List<AdminOrderListItemRestResponse> items, int page, int size, long totalElements) {

    public static PagedAdminOrdersRestResponse from(PagedResult<AdminOrderListItemResult> result) {
        return new PagedAdminOrdersRestResponse(
                result.items().stream().map(AdminOrderListItemRestResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }
}
