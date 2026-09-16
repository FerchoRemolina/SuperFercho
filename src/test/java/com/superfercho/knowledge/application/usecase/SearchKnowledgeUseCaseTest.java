package com.superfercho.knowledge.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.dto.KnowledgeSearchHit;
import com.superfercho.knowledge.application.dto.KnowledgeSearchResult;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.fakes.FakeEmbeddingPort;
import com.superfercho.knowledge.application.fakes.FakeKnowledgeVectorStore;
import com.superfercho.knowledge.application.fakes.InMemoryKnowledgeDocumentRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SearchKnowledgeUseCaseTest {

    private FakeEmbeddingPort embeddings;
    private FakeKnowledgeVectorStore vectorStore;
    private SearchKnowledgeUseCase searchKnowledge;

    @BeforeEach
    void setUp() {
        embeddings = new FakeEmbeddingPort();
        vectorStore = new FakeKnowledgeVectorStore();
        searchKnowledge = new SearchKnowledgeUseCase(embeddings, vectorStore);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldReturnEmptyWithoutEmbeddingWhenQueryIsBlank(String query) {
        KnowledgeSearchResult result = searchKnowledge.execute(new SearchKnowledgeCommand(query, 5));

        assertTrue(result.hits().isEmpty());
        assertTrue(embeddings.calls().isEmpty());
        assertTrue(vectorStore.searches().isEmpty());
    }

    @Test
    void shouldEmbedQueryAndSearchWithLimit() {
        KnowledgeSearchHit hit = new KnowledgeSearchHit(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Guía de frutas",
                "manual-interno",
                "Las frutas deben estar frescas.",
                0.91d);
        vectorStore.setHits(List.of(hit));

        KnowledgeSearchResult result = searchKnowledge.execute(new SearchKnowledgeCommand("frutas frescas", 7));

        assertEquals(List.of("frutas frescas"), embeddings.calls().get(0));
        assertEquals(1, vectorStore.searches().size());
        assertEquals(7, vectorStore.searches().get(0).limit());
        EmbeddingVector expected = new EmbeddingVector(new float[] {"frutas frescas".hashCode()});
        assertEquals(expected, vectorStore.searches().get(0).query());
        assertEquals(List.of(hit), result.hits());
    }

    @Test
    void shouldRejectInvalidLimit() {
        assertThrows(
                KnowledgeProcessingException.class,
                () -> searchKnowledge.execute(new SearchKnowledgeCommand("frutas", 0)));
        assertThrows(
                KnowledgeProcessingException.class,
                () -> searchKnowledge.execute(new SearchKnowledgeCommand("frutas", -1)));
        assertThrows(
                KnowledgeProcessingException.class,
                () -> searchKnowledge.execute(new SearchKnowledgeCommand("frutas", 21)));
        assertTrue(embeddings.calls().isEmpty());
        assertTrue(vectorStore.searches().isEmpty());
    }

    @Test
    void shouldNotUseDocumentRepositoryToFilterReady() {
        InMemoryKnowledgeDocumentRepository documents = new InMemoryKnowledgeDocumentRepository();
        searchKnowledge.execute(new SearchKnowledgeCommand("frutas", 3));

        assertTrue(documents.findAll().isEmpty());
        assertEquals(1, embeddings.calls().size());
        assertEquals(1, vectorStore.searches().size());
    }
}
