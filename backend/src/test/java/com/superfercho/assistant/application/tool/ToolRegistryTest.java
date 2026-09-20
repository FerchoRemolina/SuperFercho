package com.superfercho.assistant.application.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.exception.ToolNotAllowedException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    private final ToolRegistry registry = new ToolRegistry(List.of(new EchoTool()));

    @Test
    void shouldExecuteAllowedTool() {
        ToolResult result = registry.execute("echo", Map.of("text", "hola"));

        assertTrue(result.success());
        assertEquals("hola", result.content());
    }

    @Test
    void shouldRejectUnknownTool() {
        assertThrows(ToolNotAllowedException.class, () -> registry.execute("missing", Map.of()));
    }

    @Test
    void shouldRejectToolOutsideAllowlist() {
        assertThrows(ToolNotAllowedException.class, () -> registry.execute("checkout_admin", Map.of()));
    }

    @Test
    void shouldRejectUnknownArgument() {
        assertThrows(
                InvalidToolArgumentsException.class, () -> registry.execute("echo", Map.of("text", "hola", "extra", "x")));
    }

    @Test
    void shouldRejectUserIdArgument() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> registry.execute("echo", Map.of("text", "hola", "userId", "11111111-1111-1111-1111-111111111111")));
    }

    @Test
    void shouldRejectCustomerIdArgument() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> registry.execute("echo", Map.of("customerId", "11111111-1111-1111-1111-111111111111")));
    }

    @Test
    void shouldRejectMissingRequiredArgument() {
        assertThrows(InvalidToolArgumentsException.class, () -> registry.execute("echo", Map.of()));
    }

    private static final class EchoTool implements AssistantTool {

        @Override
        public String name() {
            return "echo";
        }

        @Override
        public String description() {
            return "Echo";
        }

        @Override
        public ToolSchema schema() {
            return ToolSchema.of(new ToolParameter("text", "string", true, "Text"));
        }

        @Override
        public ToolRisk risk() {
            return ToolRisk.QUERY;
        }

        @Override
        public ToolResult execute(ToolArguments arguments) {
            return ToolResult.success(arguments.requireText("text"));
        }
    }
}
