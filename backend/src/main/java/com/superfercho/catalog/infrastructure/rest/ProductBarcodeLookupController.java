package com.superfercho.catalog.infrastructure.rest;

import com.superfercho.catalog.application.dto.LookupProductByBarcodeQuery;
import com.superfercho.catalog.application.usecase.LookupProductByBarcodeUseCase;
import com.superfercho.catalog.infrastructure.rest.dto.BarcodeProductSuggestionRestResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/products")
public class ProductBarcodeLookupController {

    private final LookupProductByBarcodeUseCase lookupProductByBarcodeUseCase;

    public ProductBarcodeLookupController(LookupProductByBarcodeUseCase lookupProductByBarcodeUseCase) {
        this.lookupProductByBarcodeUseCase = lookupProductByBarcodeUseCase;
    }

    @GetMapping("/barcode-lookup/{barcode}")
    public BarcodeProductSuggestionRestResponse lookup(@PathVariable String barcode) {
        return BarcodeProductSuggestionRestResponse.from(
                lookupProductByBarcodeUseCase.execute(new LookupProductByBarcodeQuery(barcode)));
    }
}
