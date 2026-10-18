package com.superfercho.catalog.application.dto;

import java.util.UUID;

/** Unidades vendidas de un producto dentro de una ventana de ventas. */
public record ProductSales(UUID productId, long unitsSold) {}
