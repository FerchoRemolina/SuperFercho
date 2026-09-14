package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.StockDecrementResult;
import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.dto.UnavailableProduct;
import com.superfercho.catalog.application.port.InventoryPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryContractTest {

    private static final UUID MILK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BREAD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldExposeUnavailableProductsWithoutClaimingAtomicity() {
        InventoryPort inventoryPort = mock(InventoryPort.class);
        List<StockQuantity> requested = List.of(new StockQuantity(MILK_ID, 2), new StockQuantity(BREAD_ID, 1));
        StockDecrementResult portResult =
                StockDecrementResult.unavailable(List.of(new UnavailableProduct(MILK_ID, 2)));
        when(inventoryPort.decreaseStockAtomically(requested)).thenReturn(portResult);

        StockDecrementResult result = inventoryPort.decreaseStockAtomically(requested);

        assertFalse(result.succeeded());
        assertEquals(List.of(MILK_ID), result.unavailableProductIds());
        assertEquals(2, result.unavailableProducts().get(0).requestedQuantity());
    }

    @Test
    void shouldRepresentSuccessfulDecrementForOrders() {
        InventoryPort inventoryPort = mock(InventoryPort.class);
        List<StockQuantity> requested = List.of(new StockQuantity(MILK_ID, 1));
        when(inventoryPort.decreaseStockAtomically(requested)).thenReturn(StockDecrementResult.success());

        StockDecrementResult result = inventoryPort.decreaseStockAtomically(requested);

        assertTrue(result.succeeded());
        assertTrue(result.unavailableProducts().isEmpty());
        assertTrue(result.unavailableProductIds().isEmpty());
    }

    @Test
    void shouldDelegateStockRestoreToInventoryPort() {
        InventoryPort inventoryPort = mock(InventoryPort.class);
        List<StockQuantity> items = List.of(new StockQuantity(MILK_ID, 2));

        inventoryPort.restoreStock(items);

        verify(inventoryPort).restoreStock(items);
    }
}
