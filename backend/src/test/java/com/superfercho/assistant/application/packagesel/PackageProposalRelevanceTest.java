package com.superfercho.assistant.application.packagesel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.assistant.application.tool.ToolNames;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackageProposalRelevanceTest {

    @Test
    void keepsOfferOnTextFollowUpAndCompatibleLookups() {
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of()));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.GET_CART)));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.GET_PRODUCT)));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.SEARCH_KNOWLEDGE)));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.LIST_ORDERS)));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.LIST_ADDRESSES)));
    }

    @Test
    void detachesOnShoppingListAndCartMutations() {
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.LIST_SHOPPING_LISTS)));
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.ADD_SHOPPING_LIST_TO_CART)));
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(
                List.of(ToolNames.ADD_SHOPPING_LIST_TO_CART, ToolNames.GET_CART)));
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.ADD_CART_ITEM)));
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.CHECKOUT)));
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(ToolNames.SEARCH_PRODUCTS)));
    }

    @Test
    void shoppingListIntentDetachesEvenIfQuantityEquivalenceReruns() {
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(
                ToolNames.ADD_SHOPPING_LIST_TO_CART, ToolNames.RESOLVE_QUANTITY_EQUIVALENCE)));
        assertTrue(PackageProposalRelevance.shouldDetachActiveOffer(List.of(
                ToolNames.RESOLVE_QUANTITY_EQUIVALENCE,
                ToolNames.ADD_SHOPPING_LIST_TO_CART,
                ToolNames.GET_CART)));
    }

    @Test
    void aNewQuantityEquivalenceKeepsTheFreshOfferUnlessIntentChanged() {
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(
                List.of(ToolNames.RESOLVE_QUANTITY_EQUIVALENCE)));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(
                ToolNames.SEARCH_PRODUCTS, ToolNames.RESOLVE_QUANTITY_EQUIVALENCE)));
        assertFalse(PackageProposalRelevance.shouldDetachActiveOffer(List.of(
                ToolNames.GET_PRODUCT, ToolNames.RESOLVE_QUANTITY_EQUIVALENCE)));
    }
}
