package com.superfercho.assistant.application.tool.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.dto.ListCategoriesCommand;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.catalog.domain.model.Presentation;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogToolsTest {

    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CATEGORY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TYPE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Presentation UNIT = Presentation.of(1, PresentationUnit.UNIT);

    @Mock
    private SearchProductsUseCase searchProductsUseCase;

    @Mock
    private GetProductUseCase getProductUseCase;

    @Mock
    private ListProductsUseCase listProductsUseCase;

    @Mock
    private ListCategoriesUseCase listCategoriesUseCase;

    @Test
    void searchProductsUsesPublicCatalogView() {
        when(searchProductsUseCase.execute(any())).thenReturn(List.of(product()));

        ToolResult result = new SearchProductsTool(searchProductsUseCase).execute(ToolArguments.of(Map.of("query", "leche")));

        ArgumentCaptor<SearchProductsCommand> captor = ArgumentCaptor.forClass(SearchProductsCommand.class);
        verify(searchProductsUseCase).execute(captor.capture());
        assertEquals(CatalogView.PUBLIC, captor.getValue().view());
        assertEquals("leche", captor.getValue().text());
        assertEquals(true, result.success());
    }

    @Test
    void getProductUsesPublicCatalogView() {
        when(getProductUseCase.execute(any())).thenReturn(product());

        new GetProductTool(getProductUseCase).execute(ToolArguments.of(Map.of("productId", PRODUCT_ID.toString())));

        ArgumentCaptor<GetProductCommand> captor = ArgumentCaptor.forClass(GetProductCommand.class);
        verify(getProductUseCase).execute(captor.capture());
        assertEquals(CatalogView.PUBLIC, captor.getValue().view());
        assertEquals(PRODUCT_ID, captor.getValue().productId());
    }

    @Test
    void listProductsUsesPublicCatalogView() {
        when(listProductsUseCase.execute(any())).thenReturn(List.of());

        new ListProductsTool(listProductsUseCase)
                .execute(ToolArguments.of(Map.of("categoryId", CATEGORY_ID.toString())));

        ArgumentCaptor<ListProductsCommand> captor = ArgumentCaptor.forClass(ListProductsCommand.class);
        verify(listProductsUseCase).execute(captor.capture());
        assertEquals(CatalogView.PUBLIC, captor.getValue().view());
        assertEquals(CATEGORY_ID, captor.getValue().categoryId());
        assertEquals(null, captor.getValue().status());
    }

    @Test
    void listCategoriesUsesPublicCatalogView() {
        when(listCategoriesUseCase.execute(any())).thenReturn(List.of());

        new ListCategoriesTool(listCategoriesUseCase).execute(ToolArguments.of(Map.of()));

        ArgumentCaptor<ListCategoriesCommand> captor = ArgumentCaptor.forClass(ListCategoriesCommand.class);
        verify(listCategoriesUseCase).execute(captor.capture());
        assertEquals(CatalogView.PUBLIC, captor.getValue().view());
    }

    private static ProductResult product() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ProductResult(
                PRODUCT_ID,
                CATEGORY_ID,
                TYPE_ID,
                null,
                UNIT,
                "7701",
                "Leche",
                "Alpina",
                "1L",
                Money.cop(new BigDecimal("10.50")),
                4,
                null,
                ProductStatus.ACTIVE,
                now,
                now);
    }
}
