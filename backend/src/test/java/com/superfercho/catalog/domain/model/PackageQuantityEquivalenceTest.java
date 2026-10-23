package com.superfercho.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PackageQuantityEquivalenceTest {

    @Test
    void oneLitreAgainstFiveHundredMillilitresIsExactlyTwoPackages() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(500, PresentationUnit.ML), Presentation.of(1, PresentationUnit.L));

        assertTrue(match.compatible());
        assertTrue(match.exactMatch());
        assertEquals(PresentationUnit.ML, match.baseUnit());
        assertEquals(0, new BigDecimal("500.000").compareTo(match.packagePhysicalQuantityBase()));
        assertEquals(0, new BigDecimal("1000.000").compareTo(match.requestedPhysicalQuantityBase()));
        assertEquals(2, match.exact().packageCount());
        assertEquals(0, new BigDecimal("1000.000").compareTo(match.exact().physicalTotalBase()));
        assertEquals(0, BigDecimal.ZERO.compareTo(match.exact().differenceFromRequestedBase()));
        assertNull(match.lowerAlternative());
        assertNull(match.upperAlternative());
    }

    @Test
    void twoPointThreeKilogramsAgainstOneKilogramHasAlternativesTwoAndThree() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(1, PresentationUnit.KG),
                Presentation.of(new BigDecimal("2.3"), PresentationUnit.KG));

        assertTrue(match.compatible());
        assertFalse(match.exactMatch());
        assertNull(match.exact());
        assertEquals(2, match.lowerAlternative().packageCount());
        assertEquals(0, new BigDecimal("2000.000").compareTo(match.lowerAlternative().physicalTotalBase()));
        assertEquals(0, new BigDecimal("300.000").compareTo(match.lowerAlternative().differenceFromRequestedBase()));
        assertEquals(3, match.upperAlternative().packageCount());
        assertEquals(0, new BigDecimal("3000.000").compareTo(match.upperAlternative().physicalTotalBase()));
        assertEquals(0, new BigDecimal("-700.000").compareTo(match.upperAlternative().differenceFromRequestedBase()));
    }

    @Test
    void twoPointThreeKilogramsAgainstFiveHundredGramsHasAlternativesFourAndFive() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(500, PresentationUnit.G),
                Presentation.of(new BigDecimal("2.3"), PresentationUnit.KG));

        assertFalse(match.exactMatch());
        assertEquals(4, match.lowerAlternative().packageCount());
        assertEquals(0, new BigDecimal("2000.000").compareTo(match.lowerAlternative().physicalTotalBase()));
        assertEquals(5, match.upperAlternative().packageCount());
        assertEquals(0, new BigDecimal("2500.000").compareTo(match.upperAlternative().physicalTotalBase()));
    }

    @Test
    void twoPointThreeKilogramsAgainstSevenHundredFiftyGramsHasAlternativesThreeAndFour() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(750, PresentationUnit.G),
                Presentation.of(new BigDecimal("2.3"), PresentationUnit.KG));

        assertFalse(match.exactMatch());
        assertEquals(3, match.lowerAlternative().packageCount());
        assertEquals(0, new BigDecimal("2250.000").compareTo(match.lowerAlternative().physicalTotalBase()));
        assertEquals(0, new BigDecimal("50.000").compareTo(match.lowerAlternative().differenceFromRequestedBase()));
        assertEquals(4, match.upperAlternative().packageCount());
        assertEquals(0, new BigDecimal("3000.000").compareTo(match.upperAlternative().physicalTotalBase()));
    }

    @Test
    void massIsNotConvertedToVolume() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(500, PresentationUnit.ML), Presentation.of(1, PresentationUnit.KG));

        assertFalse(match.compatible());
        assertFalse(match.exactMatch());
        assertNull(match.exact());
        assertNull(match.lowerAlternative());
        assertNull(match.upperAlternative());
    }

    @ParameterizedTest
    @EnumSource(
            value = PresentationUnit.class,
            names = {"UNIT", "PACK", "BOX", "ROLL"})
    void discretePresentationsAreNotConverted(PresentationUnit unit) {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(1, unit), Presentation.of(1, PresentationUnit.KG));

        assertFalse(match.compatible());
        assertFalse(match.exactMatch());
        assertNull(match.exact());
    }

    @Test
    void discreteUnitsAreNotConvertedAmongThemselves() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(1, PresentationUnit.PACK), Presentation.of(2, PresentationUnit.BOX));

        assertFalse(match.compatible());
    }

    @Test
    void requestSmallerThanOnePackageOnlyOffersUpperAlternative() {
        PackageQuantityEquivalence.Match match = PackageQuantityEquivalence.match(
                Presentation.of(500, PresentationUnit.G), Presentation.of(200, PresentationUnit.G));

        assertFalse(match.exactMatch());
        assertNull(match.lowerAlternative());
        assertNotNull(match.upperAlternative());
        assertEquals(1, match.upperAlternative().packageCount());
        assertEquals(0, new BigDecimal("500.000").compareTo(match.upperAlternative().physicalTotalBase()));
    }
}
