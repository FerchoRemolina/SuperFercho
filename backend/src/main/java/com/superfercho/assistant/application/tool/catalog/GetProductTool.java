package com.superfercho.assistant.application.tool.catalog;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.GetProductCommand;
import com.superfercho.catalog.application.usecase.GetProductUseCase;

public final class GetProductTool implements AssistantTool {

    private final GetProductUseCase getProductUseCase;

    public GetProductTool(GetProductUseCase getProductUseCase) {
        this.getProductUseCase = getProductUseCase;
    }

    @Override
    public String name() {
        return ToolNames.GET_PRODUCT;
    }

    @Override
    public String description() {
        return "Get a public catalog product by id, including current price and stock.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(new ToolParameter("productId", "uuid", true, "Product id"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(String.valueOf(getProductUseCase.execute(
                new GetProductCommand(arguments.requireUuid("productId"), CatalogView.PUBLIC))));
    }
}
