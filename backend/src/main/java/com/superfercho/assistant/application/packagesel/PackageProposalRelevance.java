package com.superfercho.assistant.application.packagesel;

import com.superfercho.assistant.application.tool.ToolNames;
import java.util.List;
import java.util.Set;

/**
 * An OFFERED package proposal stays attached on a text follow-up or a catalog
 * lookup that accompanies a new quantity offer in the same turn. Shopping-list,
 * cart-mutation, checkout and cancel tools always detach, even if the model
 * also re-ran {@code resolve_quantity_equivalence} from earlier context.
 */
public final class PackageProposalRelevance {

    private static final Set<String> INCOMPATIBLE_INTENT_TOOLS = Set.of(
            ToolNames.ADD_CART_ITEM,
            ToolNames.CHANGE_CART_ITEM_QUANTITY,
            ToolNames.REMOVE_CART_ITEM,
            ToolNames.CLEAR_CART,
            ToolNames.LIST_SHOPPING_LISTS,
            ToolNames.GET_SHOPPING_LIST,
            ToolNames.CREATE_SHOPPING_LIST,
            ToolNames.RENAME_SHOPPING_LIST,
            ToolNames.ADD_SHOPPING_LIST_ITEM,
            ToolNames.CHANGE_SHOPPING_LIST_ITEM_QUANTITY,
            ToolNames.REMOVE_SHOPPING_LIST_ITEM,
            ToolNames.CLEAR_SHOPPING_LIST,
            ToolNames.ADD_SHOPPING_LIST_TO_CART,
            ToolNames.CHECKOUT,
            ToolNames.CANCEL_ORDER);

    private static final Set<String> CATALOG_BROWSE_TOOLS = Set.of(
            ToolNames.SEARCH_PRODUCTS,
            ToolNames.LIST_PRODUCTS,
            ToolNames.LIST_CATEGORIES);

    private PackageProposalRelevance() {}

    public static boolean shouldDetachActiveOffer(List<String> executedTools) {
        if (executedTools == null || executedTools.isEmpty()) {
            return false;
        }
        for (String tool : executedTools) {
            if (INCOMPATIBLE_INTENT_TOOLS.contains(tool)) {
                return true;
            }
        }
        if (executedTools.contains(ToolNames.RESOLVE_QUANTITY_EQUIVALENCE)) {
            return false;
        }
        for (String tool : executedTools) {
            if (CATALOG_BROWSE_TOOLS.contains(tool)) {
                return true;
            }
        }
        return false;
    }
}
