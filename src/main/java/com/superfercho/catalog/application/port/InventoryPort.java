package com.superfercho.catalog.application.port;

import com.superfercho.catalog.application.dto.StockDecrementResult;
import com.superfercho.catalog.application.dto.StockQuantity;
import java.util.List;

public interface InventoryPort {

    StockDecrementResult decreaseStockAtomically(List<StockQuantity> items);

    void restoreStock(List<StockQuantity> items);
}
