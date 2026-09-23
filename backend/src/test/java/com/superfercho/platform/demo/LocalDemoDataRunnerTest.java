package com.superfercho.platform.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.usecase.CreateCategoryUseCase;
import com.superfercho.catalog.application.usecase.CreateProductUseCase;
import com.superfercho.catalog.application.usecase.DeactivateCategoryUseCase;
import com.superfercho.catalog.application.usecase.DeactivateProductUseCase;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.knowledge.domain.model.KnowledgeDocument;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;

@ExtendWith(MockitoExtension.class)
class LocalDemoDataRunnerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private CreateCategoryUseCase createCategoryUseCase;

    @Mock
    private CreateProductUseCase createProductUseCase;

    @Mock
    private DeactivateProductUseCase deactivateProductUseCase;

    @Mock
    private DeactivateCategoryUseCase deactivateCategoryUseCase;

    @Mock
    private CreateDocumentUseCase createDocumentUseCase;

    @Mock
    private ListDocumentsUseCase listDocumentsUseCase;

    @Mock
    private ProcessDocumentUseCase processDocumentUseCase;

    private LocalDemoDataRunner runner;

    @BeforeEach
    void setUp() {
        runner = new LocalDemoDataRunner(
                categoryRepository,
                productRepository,
                documentRepository,
                createCategoryUseCase,
                createProductUseCase,
                deactivateProductUseCase,
                deactivateCategoryUseCase,
                createDocumentUseCase,
                listDocumentsUseCase,
                processDocumentUseCase,
                true);
    }

    @Test
    void isRestrictedToLocalProfile() {
        Profile profile = LocalDemoDataRunner.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        assertEquals(1, profile.value().length);
        assertEquals("local", profile.value()[0]);
    }

    @Test
    void skipsWhenDisabled() {
        runner = new LocalDemoDataRunner(
                categoryRepository,
                productRepository,
                documentRepository,
                createCategoryUseCase,
                createProductUseCase,
                deactivateProductUseCase,
                deactivateCategoryUseCase,
                createDocumentUseCase,
                listDocumentsUseCase,
                processDocumentUseCase,
                false);

        runner.run(new DefaultApplicationArguments());

        verify(productRepository, never()).findAll();
        verify(documentRepository, never()).findAll();
        verify(createCategoryUseCase, never()).execute(any());
        verify(createDocumentUseCase, never()).execute(any());
    }

    @Test
    void skipsCatalogAndKnowledgeWhenAlreadyPresent() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct()));
        when(documentRepository.findAll()).thenReturn(List.of(mock(KnowledgeDocument.class)));

        runner.run(new DefaultApplicationArguments());

        verify(createCategoryUseCase, never()).execute(any());
        verify(createProductUseCase, never()).execute(any());
        verify(createDocumentUseCase, never()).execute(any());
    }

    private static Product sampleProduct() {
        return Product.create(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "7700000000001",
                "Leche",
                "Colanta",
                "Descripción",
                Money.cop(new BigDecimal("1000.00")),
                1,
                null,
                ProductStatus.ACTIVE,
                NOW,
                NOW);
    }
}
