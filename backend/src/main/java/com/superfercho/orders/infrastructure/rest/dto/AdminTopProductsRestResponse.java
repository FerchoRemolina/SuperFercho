package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminProductSalesRow;
import java.util.List;
import java.util.UUID;

/**
 * Product ranking by units sold for an arbitrary [from, to) period.
 * {@code quantity} is the sum of item quantities (units), not order count.
 */
public record AdminTopProductsRestResponse(List<ProductSalesRestResponse> items) {

    public static AdminTopProductsRestResponse from(List<AdminProductSalesRow> rows) {
        return new AdminTopProductsRestResponse(
                rows.stream().map(ProductSalesRestResponse::from).toList());
    }

    public record ProductSalesRestResponse(UUID productId, String productName, long quantity) {

        public static ProductSalesRestResponse from(AdminProductSalesRow row) {
            return new ProductSalesRestResponse(row.productId(), row.productName(), row.quantity());
        }
    }
}
