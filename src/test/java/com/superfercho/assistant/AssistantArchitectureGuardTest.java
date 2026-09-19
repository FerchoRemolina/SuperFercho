package com.superfercho.assistant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AssistantArchitectureGuardTest {

    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(
            "jakarta.persistence",
            "org.springframework.data",
            "org.hibernate",
            "pgvector",
            "com.superfercho.catalog.infrastructure",
            "com.superfercho.shopping.infrastructure",
            "com.superfercho.orders.infrastructure",
            "com.superfercho.orders.application.usecase.CheckoutUseCase",
            "com.superfercho.orders.application.usecase.CancelOrderUseCase",
            "com.superfercho.knowledge.infrastructure",
            "com.superfercho.payments",
            "com.superfercho.catalog.application.port.ProductRepository",
            "com.superfercho.catalog.application.port.InventoryPort",
            "com.superfercho.knowledge.application.port.KnowledgeVectorStorePort",
            "com.superfercho.knowledge.application.port.EmbeddingPort",
            "com.superfercho.knowledge.infrastructure.rest",
            "/api/v1/knowledge/search");

    @Test
    void assistantSourcesDoNotDependOnForbiddenInfrastructure() throws IOException {
        Path root = Path.of("src/main/java/com/superfercho/assistant");
        assertTrue(Files.isDirectory(root), "assistant main sources must exist");
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            assertTrue(javaFiles.size() > 5);
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                for (String forbidden : FORBIDDEN_FRAGMENTS) {
                    assertTrue(
                            !source.contains(forbidden),
                            file + " must not depend on " + forbidden);
                }
            }
        }
    }

    @Test
    void assistantDependsOnOrdersApplicationContractsNotConcreteUseCases() throws IOException {
        String chat = Files.readString(
                Path.of("src/main/java/com/superfercho/assistant/application/service/ChatApplicationService.java"));
        String config = Files.readString(Path.of(
                "src/main/java/com/superfercho/assistant/infrastructure/configuration/AssistantUseCaseConfiguration.java"));
        assertTrue(chat.contains("com.superfercho.orders.application.port.in.CheckoutUseCase"));
        assertTrue(chat.contains("com.superfercho.orders.application.port.in.CancelOrderUseCase"));
        assertTrue(config.contains("com.superfercho.orders.application.port.in.CheckoutUseCase"));
        assertTrue(config.contains("com.superfercho.orders.application.port.in.CancelOrderUseCase"));
        assertTrue(!chat.contains("orders.infrastructure"));
        assertTrue(!config.contains("orders.infrastructure"));
    }
}
