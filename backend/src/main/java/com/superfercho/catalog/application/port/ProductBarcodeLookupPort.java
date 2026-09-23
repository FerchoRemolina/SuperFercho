package com.superfercho.catalog.application.port;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import java.util.Optional;

public interface ProductBarcodeLookupPort {

    Optional<BarcodeProductSuggestion> findByBarcode(String barcode);
}
