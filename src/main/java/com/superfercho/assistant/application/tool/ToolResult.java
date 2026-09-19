package com.superfercho.assistant.application.tool;

public record ToolResult(boolean success, String content, boolean awaitsConfirmation, String confirmationToken) {

    public static ToolResult success(String content) {
        return new ToolResult(true, content, false, null);
    }

    public static ToolResult failure(String content) {
        return new ToolResult(false, content, false, null);
    }

    public static ToolResult confirmationRequired(String content, String confirmationToken) {
        return new ToolResult(true, content, true, confirmationToken);
    }
}
