package com.superfercho.catalog.application.port;

import com.superfercho.catalog.application.dto.ProductSales;
import java.time.Instant;
import java.util.List;

/**
 * Read model de ventas consumido por el catálogo (driven port). La
 * implementación vive en el módulo de órdenes, dueño de los datos de venta.
 */
public interface SalesPort {

    /**
     * Unidades vendidas por producto en {@code [fromInclusive, toExclusive)},
     * mejor vendido primero. Los pedidos CANCELLED no contribuyen (regla
     * existente de la analítica de órdenes).
     */
    List<ProductSales> topProductsByQuantity(Instant fromInclusive, Instant toExclusive, int limit);
}
