package com.superfercho.knowledge.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.application.dto.ChunkEmbedding;
import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.port.ClockPort;
import com.superfercho.knowledge.application.port.DocumentChunkerPort;
import com.superfercho.knowledge.application.port.EmbeddingPort;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class KnowledgeApplicationPortsTest {

    @Test
    void portsOnlyExposeJdkDomainAndApplicationTypes() {
        assertPortMethods(ClockPort.class, "now");
        assertPortMethods(KnowledgeDocumentRepository.class, "save", "findById", "findAll");
        assertPortMethods(DocumentChunkerPort.class, "chunk");
        assertPortMethods(EmbeddingPort.class, "embed");
        assertPortMethods(KnowledgeVectorStorePort.class, "upsert", "deleteByDocumentId", "search");
        assertEquals(Instant.class, method(ClockPort.class, "now").getReturnType());
        assertTrue(genericReturnContains(method(EmbeddingPort.class, "embed"), EmbeddingVector.class.getName()));
        assertEquals(void.class, method(KnowledgeVectorStorePort.class, "upsert").getReturnType());
        assertEquals(
                ChunkEmbedding.class, method(KnowledgeVectorStorePort.class, "upsert").getParameterTypes()[0]);
    }

    private static void assertPortMethods(Class<?> type, String... names) {
        Set<String> actual =
                Arrays.stream(type.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
        assertEquals(Set.of(names), actual);
        Arrays.stream(type.getDeclaredMethods()).forEach(KnowledgeApplicationPortsTest::assertAllowedTypes);
    }

    private static void assertAllowedTypes(Method method) {
        assertAllowed(method.getReturnType(), method.getGenericReturnType().getTypeName());
        for (var parameter : method.getParameters()) {
            assertAllowed(parameter.getType(), parameter.getParameterizedType().getTypeName());
        }
    }

    private static void assertAllowed(Class<?> type, String typeName) {
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive() || type.getName().startsWith("java.")) {
            return;
        }
        assertTrue(
                type.getName().startsWith("com.superfercho.knowledge."),
                () -> "forbidden type on port: " + typeName);
    }

    private static Method method(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static boolean genericReturnContains(Method method, String typeName) {
        return method.getGenericReturnType().getTypeName().contains(typeName);
    }
}
