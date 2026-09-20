package com.superfercho.knowledge.application.port;

import com.superfercho.knowledge.domain.model.ChunkText;
import com.superfercho.knowledge.domain.model.DocumentContent;
import java.util.List;

public interface DocumentChunkerPort {

    List<ChunkText> chunk(DocumentContent content);
}
