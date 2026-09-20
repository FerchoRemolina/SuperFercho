package com.superfercho.assistant.application.tool.catalog;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ListProductsCommand;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;

public final class ListProductsTool implements AssistantTool {

    private final ListProductsUseCase listProductsUseCase;

    public ListProductsTool(ListProductsUseCase listProductsUseCase) {
        this.listProductsUseCase = listProductsUseCase;
    }

    @Override
    public String name() {
        return ToolNames.LIST_PRODUCTS;
    }

    @Override
    public String description() {
        return "List public catalog products, optionally filtered by category.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("categoryId", "uuid", false, "Optional category id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(listProductsUseCase.execute(new ListProductsCommand(
                arguments.optionalUuid("categoryId").orElse(null), null, CatalogView.PUBLIC))));
    }
}
