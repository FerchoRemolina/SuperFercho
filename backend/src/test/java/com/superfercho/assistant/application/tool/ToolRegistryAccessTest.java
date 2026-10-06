package com.superfercho.assistant.application.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;
import com.superfercho.assistant.application.exception.ToolAccessDeniedException;
import com.superfercho.assistant.application.tool.catalog.GetProductTool;
import com.superfercho.assistant.application.tool.catalog.ListCategoriesTool;
import com.superfercho.assistant.application.tool.catalog.ListProductsTool;
import com.superfercho.assistant.application.tool.catalog.SearchProductsTool;
import com.superfercho.assistant.application.tool.shopping.GetCartTool;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToolRegistryAccessTest {

    @Mock
    private GetCartUseCase getCartUseCase;

    private ToolRegistry registry() {
        return new ToolRegistry(List.of(
                new SearchProductsTool(mock(SearchProductsUseCase.class)),
                new GetProductTool(mock(GetProductUseCase.class)),
                new ListProductsTool(mock(ListProductsUseCase.class)),
                new ListCategoriesTool(mock(ListCategoriesUseCase.class)),
                new GetCartTool(getCartUseCase)));
    }

    @Test
    void visitorDefinitionsContainOnlyPublicTools() {
        List<String> names = registry().definitionsFor(ToolAccess.PUBLIC).stream()
                .map(LlmToolDefinition::name)
                .toList();

        org.assertj.core.api.Assertions.assertThat(names).containsExactlyInAnyOrder(
                "search_products", "get_product", "list_products", "list_categories");
    }

    @Test
    void customerDefinitionsIncludePublicAndCustomerTools() {
        List<String> names = registry().allowlistFor(ToolAccess.CUSTOMER).stream()
                .map(AssistantTool::name)
                .toList();

        org.assertj.core.api.Assertions.assertThat(names).containsExactlyInAnyOrder(
                "search_products", "get_product", "list_products", "list_categories", "get_cart");
    }

    @Test
    void visitorCannotExecuteCustomerTool() {
        ToolRegistry registry = registry();

        assertThrows(
                ToolAccessDeniedException.class,
                () -> registry.execute(ToolAccess.PUBLIC, "get_cart", Map.of()));

        verify(getCartUseCase, never()).execute();
    }

    @Test
    void visitorCanExecutePublicTool() {
        ToolResult result = registry().execute(ToolAccess.PUBLIC, "list_categories", Map.of());

        assertTrue(result.success());
    }

    @Test
    void customerCanExecuteCustomerTool() {
        when(getCartUseCase.execute()).thenReturn(null);

        ToolResult result = registry().execute(ToolAccess.CUSTOMER, "get_cart", Map.of());

        assertTrue(result.success());
        verify(getCartUseCase).execute();
    }
}
