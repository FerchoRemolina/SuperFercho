package com.superfercho.assistant.application.tool.identity;

import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;

public final class ListAddressesTool implements AssistantTool {

    private final ListAddressesUseCase listAddressesUseCase;

    public ListAddressesTool(ListAddressesUseCase listAddressesUseCase) {
        this.listAddressesUseCase = listAddressesUseCase;
    }

    @Override
    public String name() {
        return ToolNames.LIST_ADDRESSES;
    }

    @Override
    public String description() {
        return "List shipping addresses of the authenticated customer.";
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
        return ToolResult.success(String.valueOf(listAddressesUseCase.execute()));
    }
}
