package com.superfercho.assistant.application.tool.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.assistant.application.tool.ToolAccess;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.catalog.application.dto.QuantityEquivalenceCommand;
import com.superfercho.catalog.application.dto.QuantityEquivalenceResult;
import com.superfercho.catalog.application.dto.QuantityEquivalenceStatus;
import com.superfercho.catalog.application.dto.QuantityPackageOption;
import com.superfercho.catalog.application.usecase.ResolveProductQuantityEquivalenceUseCase;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolveQuantityEquivalenceToolTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ResolveProductQuantityEquivalenceUseCase useCase;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ClockPort clockPort;

    private InMemoryPackageSelectionProposalStore proposals;
    private ResolveQuantityEquivalenceTool tool;
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        proposals = new InMemoryPackageSelectionProposalStore();
        tool = new ResolveQuantityEquivalenceTool(useCase, currentUserProvider, clockPort, proposals);
        registry = new ToolRegistry(List.of(tool));
        org.mockito.Mockito.lenient()
                .when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedUserException());
    }

    @Test
    void queryDoesNotMutateTheCart() {
        AddProductToCartUseCase addProductToCartUseCase = mock(AddProductToCartUseCase.class);
        when(useCase.execute(any())).thenReturn(exactResult());

        ToolResult result = registry.execute(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                Map.of("productId", PRODUCT_ID.toString(), "quantity", new BigDecimal("1"), "unit", "L"));

        assertTrue(result.success());
        assertFalse(result.awaitsConfirmation());
        assertEquals(ToolRisk.QUERY, tool.risk());
        assertEquals(ToolAccess.PUBLIC, tool.access());
        verify(addProductToCartUseCase, never()).execute(any());
        ArgumentCaptor<QuantityEquivalenceCommand> captor = ArgumentCaptor.forClass(QuantityEquivalenceCommand.class);
        verify(useCase).execute(captor.capture());
        assertEquals(PRODUCT_ID, captor.getValue().productId());
        assertEquals(PresentationUnit.L, captor.getValue().unit());
        assertTrue(result.content().contains("EXACT MATCH"));
        assertTrue(result.content().contains("not a cart mutation"));
    }

    @Test
    void distinguishesInexactAlternativesFromAPurchase() {
        when(useCase.execute(any())).thenReturn(inexactResult());

        ToolResult result = registry.execute(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                Map.of("productId", PRODUCT_ID.toString(), "quantity", "2.3", "unit", "KG"));

        assertTrue(result.success());
        assertTrue(result.content().contains("NO EXACT MATCH"));
        assertTrue(result.content().contains("not a purchase"));
        assertFalse(result.content().contains("exactPackageCount=2"));
    }

    @Test
    void rejectsUnknownUnitAndUserIdentityArguments() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> registry.execute(
                        ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                        Map.of("productId", PRODUCT_ID.toString(), "quantity", 1, "unit", "UNIT")));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> registry.execute(
                        ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                        Map.of(
                                "productId",
                                PRODUCT_ID.toString(),
                                "quantity",
                                1,
                                "unit",
                                "KG",
                                "userId",
                                "11111111-1111-1111-1111-111111111111")));
        verify(useCase, never()).execute(any());
    }

    @Test
    void offersExactAndInexactSellableResultsForTheAuthenticatedUser() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant now = Instant.parse("2026-10-23T18:00:00Z");
        org.mockito.Mockito.doReturn(userId).when(currentUserProvider).getCurrentUserId();
        org.mockito.Mockito.doReturn(now).when(clockPort).currentTime();
        when(useCase.execute(any())).thenReturn(exactResult());

        registry.execute(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                Map.of("productId", PRODUCT_ID.toString(), "quantity", new BigDecimal("1"), "unit", "L"));

        var offered = proposals.findActiveOffered(userId, now).orElseThrow();
        assertEquals(PRODUCT_ID, offered.productId());
        assertEquals(PackageSelectionProposalStatus.OFFERED, offered.status());
        assertTrue(offered.allows(2));

        when(useCase.execute(any())).thenReturn(inexactSelectableResult());
        registry.execute(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                Map.of("productId", PRODUCT_ID.toString(), "quantity", "2.3", "unit", "KG"));
        var replaced = proposals.findActiveOffered(userId, now).orElseThrow();
        assertTrue(replaced.allows(2));
        assertTrue(replaced.allows(3));
        assertEquals(PackageSelectionProposalStatus.SUPERSEDED, proposals.findById(offered.proposalId(), now).orElseThrow().status());
    }

    @Test
    void doesNotOfferIncompatibleOrUnavailableResults() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant now = Instant.parse("2026-10-23T18:00:00Z");
        when(useCase.execute(any())).thenReturn(QuantityEquivalenceResult.notFound(PRODUCT_ID));

        registry.execute(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                Map.of("productId", PRODUCT_ID.toString(), "quantity", new BigDecimal("1"), "unit", "KG"));
        assertTrue(proposals.findActiveOffered(userId, now).isEmpty());

        when(useCase.execute(any())).thenReturn(incompatibleResult());
        registry.execute(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                Map.of("productId", PRODUCT_ID.toString(), "quantity", new BigDecimal("1"), "unit", "L"));
        assertTrue(proposals.findActiveOffered(userId, now).isEmpty());
    }

    @Test
    void rejectsZeroNegativeAndNonFiniteQuantity() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", 0)).requirePositiveDecimal("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", new BigDecimal("-1")))
                        .requirePositiveDecimal("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", Double.POSITIVE_INFINITY))
                        .requirePositiveDecimal("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", Double.NaN)).requirePositiveDecimal("quantity"));
    }

    private static QuantityEquivalenceResult exactResult() {
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.EXACT,
                "EXACT MATCH. This is not a cart mutation. Communicate the package count and wait for explicit acceptance of that package count before add_cart_item.",
                PRODUCT_ID,
                "Leche",
                Presentation.of(500, PresentationUnit.ML),
                Money.cop(new BigDecimal("3500.00")),
                PresentationUnit.ML,
                new BigDecimal("500.000"),
                new BigDecimal("1.000"),
                PresentationUnit.L,
                new BigDecimal("1000.000"),
                true,
                2,
                new BigDecimal("1000.000"),
                null,
                null,
                8,
                true,
                "AVAILABLE",
                null);
    }

    private static QuantityEquivalenceResult inexactResult() {
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.INEXACT,
                "NO EXACT MATCH. Alternatives are whole packages, not a purchase. Do not call add_cart_item until the customer chooses a package count. A generic yes is not a choice.",
                PRODUCT_ID,
                "Arroz",
                Presentation.of(1, PresentationUnit.KG),
                Money.cop(new BigDecimal("4500.00")),
                PresentationUnit.G,
                new BigDecimal("1000.000"),
                new BigDecimal("2.300"),
                PresentationUnit.KG,
                new BigDecimal("2300.000"),
                false,
                null,
                null,
                null,
                null,
                10,
                true,
                null,
                null);
    }

    private static QuantityEquivalenceResult inexactSelectableResult() {
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.INEXACT,
                "NO EXACT MATCH. Alternatives are whole packages, not a purchase. Do not call add_cart_item until the customer chooses a package count. A generic yes is not a choice.",
                PRODUCT_ID,
                "Arroz",
                Presentation.of(1, PresentationUnit.KG),
                Money.cop(new BigDecimal("4500.00")),
                PresentationUnit.G,
                new BigDecimal("1000.000"),
                new BigDecimal("2.300"),
                PresentationUnit.KG,
                new BigDecimal("2300.000"),
                false,
                null,
                null,
                new QuantityPackageOption(
                        2,
                        new BigDecimal("2000.000"),
                        PresentationUnit.G,
                        new BigDecimal("300.000"),
                        "AVAILABLE",
                        null),
                new QuantityPackageOption(
                        3,
                        new BigDecimal("3000.000"),
                        PresentationUnit.G,
                        new BigDecimal("-700.000"),
                        "AVAILABLE",
                        null),
                10,
                true,
                null,
                null);
    }

    private static QuantityEquivalenceResult incompatibleResult() {
        return new QuantityEquivalenceResult(
                QuantityEquivalenceStatus.INCOMPATIBLE_UNITS,
                "INCOMPATIBLE UNITS. Do not invent a conversion. This SKU is sold by its catalog presentation, not by the requested unit.",
                PRODUCT_ID,
                "Leche",
                Presentation.of(500, PresentationUnit.ML),
                Money.cop(new BigDecimal("3500.00")),
                null,
                null,
                new BigDecimal("1.000"),
                PresentationUnit.KG,
                null,
                false,
                null,
                null,
                null,
                null,
                8,
                true,
                null,
                "INCOMPATIBLE_UNITS");
    }
}
