package com.superfercho.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.dto.BarcodeProductSuggestion;
import com.superfercho.catalog.application.dto.LookupProductByBarcodeQuery;
import com.superfercho.catalog.application.exception.BarcodeLookupFailedException;
import com.superfercho.catalog.application.exception.BarcodeLookupNotFoundException;
import com.superfercho.catalog.application.exception.InvalidBarcodeException;
import com.superfercho.catalog.application.port.ProductBarcodeLookupPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LookupProductByBarcodeUseCaseTest {

    @Mock
    private ProductBarcodeLookupPort port;

    private LookupProductByBarcodeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LookupProductByBarcodeUseCase(port);
    }

    @Test
    void returnsSuggestionForValidBarcode() {
        when(port.findByBarcode("3017620422003"))
                .thenReturn(Optional.of(new BarcodeProductSuggestion(
                        "3017620422003", "Nutella", "Ferrero", "Hazelnut spread", "https://img.test/n.png")));

        BarcodeProductSuggestion suggestion =
                useCase.execute(new LookupProductByBarcodeQuery(" 3017620422003 "));

        assertEquals("Nutella", suggestion.name());
        assertEquals("Ferrero", suggestion.brand());
        verify(port).findByBarcode("3017620422003");
    }

    @Test
    void throwsNotFoundWhenPortReturnsEmpty() {
        when(port.findByBarcode("0000000000000")).thenReturn(Optional.empty());

        assertThrows(
                BarcodeLookupNotFoundException.class,
                () -> useCase.execute(new LookupProductByBarcodeQuery("0000000000000")));
    }

    @Test
    void propagatesProviderFailure() {
        when(port.findByBarcode("3017620422003"))
                .thenThrow(new BarcodeLookupFailedException("barcode lookup provider request failed"));

        assertThrows(
                BarcodeLookupFailedException.class,
                () -> useCase.execute(new LookupProductByBarcodeQuery("3017620422003")));
    }

    @Test
    void rejectsBlankBarcode() {
        assertThrows(InvalidBarcodeException.class, () -> useCase.execute(new LookupProductByBarcodeQuery("  ")));
        verifyNoInteractions(port);
    }

    @Test
    void rejectsNonDigitBarcode() {
        assertThrows(InvalidBarcodeException.class, () -> useCase.execute(new LookupProductByBarcodeQuery("abc12345")));
        verifyNoInteractions(port);
    }
}
