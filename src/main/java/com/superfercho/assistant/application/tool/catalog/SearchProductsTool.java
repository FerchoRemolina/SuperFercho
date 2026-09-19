package com.superfercho.assistant.application.tool.catalog;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.SearchProductsCommand;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import java.util.List;

public final class SearchProductsTool implements AssistantTool {

    private final SearchProductsUseCase searchProductsUseCase;

    public SearchProductsTool(SearchProductsUseCase searchProductsUseCase) {
        this.searchProductsUseCase = searchProductsUseCase;
    }

    @Override
    public String name() {
        return ToolNames.SEARCH_PRODUCTS;
    }

    @Override
    public String description() {
        return "Search public catalog products by name, brand, or barcode.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("query", "string", true, "Search text"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        List<ProductResult> products = searchProductsUseCase.execute(
                new SearchProductsCommand(arguments.requireText("query"), CatalogView.PUBLIC));
        return ToolResult.success("products=" + products);
    }
}
