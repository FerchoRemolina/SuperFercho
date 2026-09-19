package com.superfercho.assistant.application.dto.llm;

import com.superfercho.assistant.domain.model.MessageRole;
import java.util.List;
import java.util.Map;

public record LlmMessage(
        MessageRole role,
        String content,
        List<LlmToolCall> toolCalls,
        String toolCallId,
        String toolName) {

    public LlmMessage {
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(MessageRole.USER, content, List.of(), null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(MessageRole.ASSISTANT, content, List.of(), null, null);
    }

    public static LlmMessage assistantToolCalls(List<LlmToolCall> toolCalls) {
        return new LlmMessage(MessageRole.ASSISTANT, "", List.copyOf(toolCalls), null, null);
    }

    public static LlmMessage tool(String toolCallId, String toolName, String content) {
        return new LlmMessage(MessageRole.TOOL, content, List.of(), toolCallId, toolName);
    }

    public record LlmToolCall(String id, String name, Map<String, Object> arguments) {

        public LlmToolCall {
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        }
    }
}
