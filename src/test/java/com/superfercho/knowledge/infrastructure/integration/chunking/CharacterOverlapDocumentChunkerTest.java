package com.superfercho.knowledge.infrastructure.integration.chunking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.domain.exception.InvalidDocumentException;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import java.util.List;
import org.junit.jupiter.api.Test;

class CharacterOverlapDocumentChunkerTest {

    private final CharacterOverlapDocumentChunker chunker = new CharacterOverlapDocumentChunker();

    @Test
    void shouldRejectBlankContentUsingDocumentContentRules() {
        assertThrows(InvalidDocumentException.class, () -> new DocumentContent(""));
        assertThrows(InvalidDocumentException.class, () -> new DocumentContent("   "));
        assertThrows(KnowledgeProcessingException.class, () -> chunker.chunk(null));
    }

    @Test
    void shouldReturnSingleChunkForShortContent() {
        List<ChunkText> chunks = chunker.chunk(new DocumentContent("hola"));

        assertEquals(1, chunks.size());
        assertEquals("hola", chunks.get(0).value());
    }

    @Test
    void shouldReturnSingleChunkWhenContentIsSmallerThanTarget() {
        String content = "palabra ".repeat(20).trim();
        assertTrue(content.length() < CharacterOverlapDocumentChunker.TARGET_SIZE);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertEquals(1, chunks.size());
        assertEquals(content, chunks.get(0).value());
    }

    @Test
    void shouldReturnSingleChunkWhenContentEqualsTargetSize() {
        String content = "a".repeat(CharacterOverlapDocumentChunker.TARGET_SIZE);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertEquals(1, chunks.size());
        assertEquals(content, chunks.get(0).value());
    }

    @Test
    void shouldSplitContentLargerThanTarget() {
        String content = "a".repeat(CharacterOverlapDocumentChunker.TARGET_SIZE + 1);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertTrue(chunks.size() >= 2);
        assertFalse(chunks.get(0).value().isBlank());
        assertFalse(chunks.get(1).value().isBlank());
    }

    @Test
    void shouldProduceMultipleChunksPreservingOrderAndOverlap() {
        String content = "a".repeat(2000);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertEquals(3, chunks.size());
        assertEquals("a".repeat(1000), chunks.get(0).value());
        assertEquals("a".repeat(1000), chunks.get(1).value());
        assertEquals("a".repeat(300), chunks.get(2).value());
        assertEquals(
                chunks.get(0).value().substring(chunks.get(0).value().length() - CharacterOverlapDocumentChunker.OVERLAP),
                chunks.get(1).value().substring(0, CharacterOverlapDocumentChunker.OVERLAP));
        assertEquals(
                chunks.get(1).value().substring(chunks.get(1).value().length() - CharacterOverlapDocumentChunker.OVERLAP),
                chunks.get(2).value().substring(0, CharacterOverlapDocumentChunker.OVERLAP));
    }

    @Test
    void shouldNotProduceEmptyChunks() {
        String content = "bloque\n\n".repeat(200);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertFalse(chunks.isEmpty());
        for (ChunkText chunk : chunks) {
            assertFalse(chunk.value().isBlank());
        }
    }

    @Test
    void shouldBeDeterministic() {
        String content = "canción piñata\n".repeat(80);
        DocumentContent documentContent = new DocumentContent(content);

        List<ChunkText> first = chunker.chunk(documentContent);
        List<ChunkText> second = chunker.chunk(documentContent);

        assertEquals(first, second);
    }

    @Test
    void shouldHardSplitLongWordsWithoutWhitespace() {
        String content = "w".repeat(1200);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertEquals(2, chunks.size());
        assertEquals(1000, chunks.get(0).value().length());
        assertEquals(content.substring(1000 - CharacterOverlapDocumentChunker.OVERLAP), chunks.get(1).value());
    }

    @Test
    void shouldRespectNewlinesAsWordBoundaries() {
        String paragraph = "linea uno\nlinea dos\nlinea tres\n";
        String content = paragraph.repeat(50);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).value().contains("linea uno"));
        assertTrue(chunks.get(1).value().contains("linea"));
        for (ChunkText chunk : chunks) {
            assertFalse(chunk.value().isBlank());
        }
    }

    @Test
    void shouldPreserveUnicodeCharacters() {
        String content = "canción piñata café 日本語 ".repeat(60);

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertTrue(chunks.size() >= 2);
        assertTrue(content.startsWith(chunks.get(0).value()));
        assertTrue(content.endsWith(chunks.get(chunks.size() - 1).value()));
        assertTrue(chunks.get(0).value().contains("canción"));
        assertTrue(chunks.get(0).value().contains("日本語"));
    }

    @Test
    void shouldPreferWordBoundariesWhenPossible() {
        String content = "manzana ".repeat(200).trim();

        List<ChunkText> chunks = chunker.chunk(new DocumentContent(content));

        assertTrue(chunks.size() >= 2);
        assertFalse(chunks.get(0).value().endsWith("manz"));
        assertTrue(chunks.get(0).value().endsWith(" ") || chunks.get(0).value().endsWith("manzana"));
    }
}
