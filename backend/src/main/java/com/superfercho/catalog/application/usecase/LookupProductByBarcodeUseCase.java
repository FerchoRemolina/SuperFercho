package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import com.superfercho.catalog.application.dto.LookupProductByBarcodeQuery;
import com.superfercho.catalog.application.exception.BarcodeLookupNotFoundException;
import com.superfercho.catalog.application.exception.InvalidBarcodeException;
import com.superfercho.catalog.application.port.ProductBarcodeLookupPort;

public final class LookupProductByBarcodeUseCase {

    private final ProductBarcodeLookupPort productBarcodeLookupPort;

    public LookupProductByBarcodeUseCase(ProductBarcodeLookupPort productBarcodeLookupPort) {
        this.productBarcodeLookupPort = productBarcodeLookupPort;
    }

    public BarcodeProductSuggestion execute(LookupProductByBarcodeQuery query) {
        if (query == null) {
            throw new InvalidBarcodeException("barcode cannot be blank");
        }
        String barcode = normalizeBarcode(query.barcode());
        return productBarcodeLookupPort
                .findByBarcode(barcode)
                .orElseThrow(() -> new BarcodeLookupNotFoundException(barcode));
    }

    static String normalizeBarcode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidBarcodeException("barcode cannot be blank");
        }
        String barcode = raw.trim();
        if (!barcode.chars().allMatch(Character::isDigit)) {
            throw new InvalidBarcodeException("barcode must contain only digits");
        }
        if (barcode.length() < 8 || barcode.length() > 18) {
            throw new InvalidBarcodeException("barcode length is invalid");
        }
        return barcode;
    }
}
