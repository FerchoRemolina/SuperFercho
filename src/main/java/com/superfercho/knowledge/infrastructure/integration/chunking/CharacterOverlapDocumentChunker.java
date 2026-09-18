package com.superfercho.knowledge.infrastructure.integration.chunking;

import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.DocumentChunkerPort;
import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CharacterOverlapDocumentChunker implements DocumentChunkerPort {

    static final int TARGET_SIZE = 1000;
    static final int OVERLAP = 150;

    @Override
    public List<ChunkText> chunk(DocumentContent content) {
        if (content == null) {
            throw new KnowledgeProcessingException("document content cannot be null");
        }
        String text = content.value();
        List<ChunkText> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();
        while (start < length) {
            int end = Math.min(start + TARGET_SIZE, length);
            if (end < length) {
                int breakAt = lastWhitespaceAfter(text, start, end);
                if (breakAt > start) {
                    end = breakAt;
                }
            }
            String slice = text.substring(start, end);
            if (!slice.isBlank()) {
                chunks.add(new ChunkText(slice));
            }
            if (end >= length) {
                break;
            }
            int nextStart = end - OVERLAP;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return List.copyOf(chunks);
    }

    private static int lastWhitespaceAfter(String text, int start, int end) {
        for (int index = end - 1; index > start; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index + 1;
            }
        }
        return -1;
    }
}
