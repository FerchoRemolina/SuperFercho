package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.catalog.domain.exception.InvalidPresentationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PresentationTest {

    @Test
    void shouldCreateValidPresentation() {
        Presentation presentation = Presentation.of(new BigDecimal("900"), PresentationUnit.ML);

        assertEquals(new BigDecimal("900.000"), presentation.quantity());
        assertEquals(PresentationUnit.ML, presentation.unit());
    }

    @Test
    void shouldAcceptIntegerFactory() {
        Presentation presentation = Presentation.of(12, PresentationUnit.ROLL);

        assertEquals(new BigDecimal("12.000"), presentation.quantity());
        assertEquals(PresentationUnit.ROLL, presentation.unit());
    }

    @Test
    void shouldRejectZeroOrNegativeQuantity() {
        assertThrows(
                InvalidPresentationException.class,
                () -> Presentation.of(0, PresentationUnit.UNIT));
        assertThrows(
                InvalidPresentationException.class,
                () -> Presentation.of(new BigDecimal("-1"), PresentationUnit.KG));
    }

    @Test
    void shouldRejectNullQuantityOrUnit() {
        assertThrows(NullPointerException.class, () -> new Presentation(null, PresentationUnit.L));
        assertThrows(
                NullPointerException.class, () -> new Presentation(BigDecimal.ONE, null));
    }

    @Test
    void shouldRejectExcessScale() {
        assertThrows(
                InvalidPresentationException.class,
                () -> Presentation.of(new BigDecimal("1.2345"), PresentationUnit.KG));
    }
}
