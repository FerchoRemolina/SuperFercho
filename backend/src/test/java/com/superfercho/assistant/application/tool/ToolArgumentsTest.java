package com.superfercho.assistant.application.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolArgumentsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void requireIntAcceptsIntegerAndIntegralJsonNumbers() throws Exception {
        assertEquals(2, ToolArguments.of(Map.of("quantity", 2)).requirePositiveInt("quantity"));
        assertEquals(2, ToolArguments.of(Map.of("quantity", 2L)).requirePositiveInt("quantity"));
        assertEquals(2, ToolArguments.of(json("{\"quantity\":2}")).requirePositiveInt("quantity"));
        assertEquals(Integer.class, json("{\"quantity\":2}").get("quantity").getClass());
        assertEquals(2, ToolArguments.of(json("{\"quantity\":2.0}")).requirePositiveInt("quantity"));
        assertEquals(Double.class, json("{\"quantity\":2.0}").get("quantity").getClass());
        assertEquals(2, ToolArguments.of(Map.of("quantity", 2.0d)).requirePositiveInt("quantity"));
        assertEquals(2, ToolArguments.of(Map.of("quantity", new BigDecimal("2.0"))).requirePositiveInt("quantity"));
        assertEquals(2, ToolArguments.of(Map.of("quantity", BigInteger.TWO)).requirePositiveInt("quantity"));
        assertEquals(2, ToolArguments.of(Map.of("quantity", "2")).requirePositiveInt("quantity"));
    }

    @Test
    void requireIntRejectsFractionalDecimals() throws Exception {
        assertIntegerRejected(Map.of("quantity", 2.9d));
        assertIntegerRejected(Map.of("quantity", 1.5d));
        assertIntegerRejected(Map.of("quantity", -0.5d));
        assertIntegerRejected(json("{\"quantity\":2.9}"));
        assertIntegerRejected(Map.of("quantity", new BigDecimal("2.9")));
        assertIntegerRejected(Map.of("quantity", "2.9"));
    }

    @Test
    void requirePositiveDecimalAcceptsFiniteValuesAndRejectsInvalidOnes() {
        assertEquals(0, new BigDecimal("2.3").compareTo(
                ToolArguments.of(Map.of("quantity", new BigDecimal("2.3"))).requirePositiveDecimal("quantity")));
        assertEquals(0, new BigDecimal("1").compareTo(
                ToolArguments.of(Map.of("quantity", 1)).requirePositiveDecimal("quantity")));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", 0)).requirePositiveDecimal("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", Double.NaN)).requirePositiveDecimal("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("unit", "CUP")).requireOneOf("unit", java.util.Set.of("G", "KG", "ML", "L")));
    }

    @Test
    void requirePositiveIntRejectsZeroAndNegatives() {
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", 0)).requirePositiveInt("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", -1)).requirePositiveInt("quantity"));
    }

    @Test
    void requireNonNegativeIntAcceptsZeroAndRejectsNegatives() {
        assertEquals(0, ToolArguments.of(Map.of("quantity", 0)).requireNonNegativeInt("quantity"));
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("quantity", -1)).requireNonNegativeInt("quantity"));
    }

    @Test
    void requireIntRejectsOverflowNullAndIncompatibleTypes() throws Exception {
        assertIntegerRejected(Map.of("quantity", 2147483648L));
        assertIntegerRejected(json("{\"quantity\":2147483648}"));
        assertIntegerRejected(Map.of("quantity", Integer.MIN_VALUE - 1L));
        assertIntegerRejected(Map.of("quantity", Double.POSITIVE_INFINITY));
        assertIntegerRejected(Map.of("quantity", Double.NaN));
        assertIntegerRejected(Map.of("quantity", "abc"));
        assertIntegerRejected(Map.of("quantity", true));
        assertIntegerRejected(Map.of("quantity", List.of(1)));
        assertIntegerRejected(Map.of("quantity", Map.of("n", 1)));

        InvalidToolArgumentsException required = assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of()).requirePositiveInt("quantity"));
        assertEquals("quantity is required", required.getMessage());

        Map<String, Object> withNull = new HashMap<>();
        withNull.put("quantity", null);
        assertThrows(NullPointerException.class, () -> ToolArguments.of(withNull));
    }

    @Test
    void optionalIntUsesTheSameExactIntegerParsing() {
        assertEquals(0, ToolArguments.of(Map.of("page", 0)).optionalInt("page").orElseThrow());
        assertThrows(
                InvalidToolArgumentsException.class,
                () -> ToolArguments.of(Map.of("page", 1.5d)).optionalInt("page"));
    }

    private static void assertIntegerRejected(Map<String, Object> raw) {
        InvalidToolArgumentsException exception = assertThrows(
                InvalidToolArgumentsException.class, () -> ToolArguments.of(raw).requirePositiveInt("quantity"));
        assertEquals("quantity must be an integer", exception.getMessage());
    }

    private static Map<String, Object> json(String payload) throws Exception {
        return JSON.readValue(payload, new TypeReference<>() {});
    }
}
