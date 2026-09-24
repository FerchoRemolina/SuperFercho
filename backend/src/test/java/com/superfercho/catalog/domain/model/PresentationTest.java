package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldNormalizeMassAndVolumeForComparisonWithoutChangingStoredValues() {
        Presentation oneKg = Presentation.of(1, PresentationUnit.KG);
        Presentation fiveHundredG = Presentation.of(500, PresentationUnit.G);
        Presentation oneL = Presentation.of(1, PresentationUnit.L);
        Presentation nineHundredMl = Presentation.of(900, PresentationUnit.ML);

        assertEquals(0, Presentation.of(new BigDecimal("1000"), PresentationUnit.G)
                .normalizedComparableQuantity()
                .compareTo(oneKg.normalizedComparableQuantity()));
        assertEquals(PresentationDimension.MASS, oneKg.dimension());
        assertEquals(PresentationDimension.VOLUME, oneL.dimension());
        assertEquals(new BigDecimal("1.000"), oneKg.quantity());
        assertEquals(PresentationUnit.KG, oneKg.unit());

        assertTrue(Presentation.catalogOrder().compare(oneKg, fiveHundredG) < 0);
        assertTrue(Presentation.catalogOrder().compare(oneL, nineHundredMl) < 0);
        assertTrue(Presentation.catalogOrder().compare(oneKg, oneL) < 0);
    }

    @Test
    void shouldOrderDiscreteByUnitNameThenQuantityDescending() {
        Presentation twoPack = Presentation.of(2, PresentationUnit.PACK);
        Presentation onePack = Presentation.of(1, PresentationUnit.PACK);
        Presentation oneBox = Presentation.of(1, PresentationUnit.BOX);

        assertTrue(Presentation.catalogOrder().compare(oneBox, twoPack) < 0);
        assertTrue(Presentation.catalogOrder().compare(twoPack, onePack) < 0);
    }
}
