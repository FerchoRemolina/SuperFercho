package com.superfercho.catalog.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import com.superfercho.catalog.application.exception.BarcodeLookupFailedException;
import com.superfercho.catalog.application.exception.BarcodeLookupNotFoundException;
import com.superfercho.catalog.application.exception.InvalidBarcodeException;
import com.superfercho.catalog.application.usecase.LookupProductByBarcodeUseCase;
import com.superfercho.platform.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductBarcodeLookupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CatalogExceptionHandler.class, ApiExceptionHandler.class})
class ProductBarcodeLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LookupProductByBarcodeUseCase lookupProductByBarcodeUseCase;

    @Test
    void returnsSuggestion() throws Exception {
        when(lookupProductByBarcodeUseCase.execute(any()))
                .thenReturn(new BarcodeProductSuggestion(
                        "3017620422003", "Nutella", "Ferrero", "Spread", "https://img.test/n.png"));

        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", "3017620422003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("3017620422003"))
                .andExpect(jsonPath("$.name").value("Nutella"))
                .andExpect(jsonPath("$.brand").value("Ferrero"))
                .andExpect(jsonPath("$.description").value("Spread"))
                .andExpect(jsonPath("$.imageUrl").value("https://img.test/n.png"));
    }

    @Test
    void returnsNotFound() throws Exception {
        when(lookupProductByBarcodeUseCase.execute(any()))
                .thenThrow(new BarcodeLookupNotFoundException("00000000"));

        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", "00000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BARCODE_LOOKUP_NOT_FOUND"));
    }

    @Test
    void returnsInvalidBarcode() throws Exception {
        when(lookupProductByBarcodeUseCase.execute(any()))
                .thenThrow(new InvalidBarcodeException("barcode cannot be blank"));

        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BARCODE"));
    }

    @Test
    void returnsBadGatewayOnProviderFailure() throws Exception {
        when(lookupProductByBarcodeUseCase.execute(any()))
                .thenThrow(new BarcodeLookupFailedException("barcode lookup provider request failed"));

        mockMvc.perform(get("/api/v1/products/barcode-lookup/{barcode}", "3017620422003"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("BARCODE_LOOKUP_FAILED"));
    }
}
