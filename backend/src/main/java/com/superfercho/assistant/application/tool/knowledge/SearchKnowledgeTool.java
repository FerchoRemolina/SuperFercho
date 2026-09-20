package com.superfercho.assistant.application.tool.knowledge;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;

public final class SearchKnowledgeTool implements AssistantTool {

    private static final int DEFAULT_LIMIT = 5;

    private final SearchKnowledgeUseCase searchKnowledgeUseCase;

    public SearchKnowledgeTool(SearchKnowledgeUseCase searchKnowledgeUseCase) {
        this.searchKnowledgeUseCase = searchKnowledgeUseCase;
    }

    @Override
    public String name() {
        return ToolNames.SEARCH_KNOWLEDGE;
    }

    @Override
    public String description() {
        return "Search documentary knowledge such as recipes and guides. Not a source of prices or stock.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("query", "string", true, "Search query"),
                new ToolParameter("limit", "integer", false, "Maximum hits between 1 and 20"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        int limit = arguments.optionalPositiveInt("limit").orElse(DEFAULT_LIMIT);
        return ToolResult.success(String.valueOf(
                searchKnowledgeUseCase.execute(new SearchKnowledgeCommand(arguments.requireText("query"), limit))));
    }
}
