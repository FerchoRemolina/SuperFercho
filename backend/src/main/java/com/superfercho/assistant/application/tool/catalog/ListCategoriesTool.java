package com.superfercho.assistant.application.tool.catalog;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.catalog.application.dto.CatalogView;
import com.superfercho.catalog.application.dto.ListCategoriesCommand;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;

public final class ListCategoriesTool implements AssistantTool {

    private final ListCategoriesUseCase listCategoriesUseCase;

    public ListCategoriesTool(ListCategoriesUseCase listCategoriesUseCase) {
        this.listCategoriesUseCase = listCategoriesUseCase;
    }

    @Override
    public String name() {
        return ToolNames.LIST_CATEGORIES;
    }

    @Override
    public String description() {
        return "List public catalog categories.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of();
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        return ToolResult.success(
                String.valueOf(listCategoriesUseCase.execute(new ListCategoriesCommand(CatalogView.PUBLIC))));
    }
}
