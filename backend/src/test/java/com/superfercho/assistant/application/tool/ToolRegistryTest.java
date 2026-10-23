package com.superfercho.assistant.application.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void shouldAcceptExactIntegerAndRejectFractionalQuantity() throws Exception {
        ToolRegistry quantities = new ToolRegistry(List.of(new QuantityTool()));
        ObjectMapper mapper = new ObjectMapper();

        ToolResult accepted = quantities.execute(
                "qty", mapper.readValue("{\"quantity\":3}", new TypeReference<Map<String, Object>>() {}));
        assertTrue(accepted.success());
        assertEquals("3", accepted.content());

        Map<String, Object> fractional =
                mapper.readValue("{\"quantity\":2.9}", new TypeReference<Map<String, Object>>() {});
        assertThrows(InvalidToolArgumentsException.class, () -> quantities.execute("qty", fractional));
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

    private static final class QuantityTool implements AssistantTool {

        @Override
        public String name() {
            return "qty";
        }

        @Override
        public String description() {
            return "Quantity";
        }

        @Override
        public ToolSchema schema() {
            return ToolSchema.of(new ToolParameter("quantity", "integer", true, "Quantity"));
        }

        @Override
        public ToolRisk risk() {
            return ToolRisk.LOW_RISK;
        }

        @Override
        public ToolResult execute(ToolArguments arguments) {
            return ToolResult.success(String.valueOf(arguments.requirePositiveInt("quantity")));
        }
    }
}
