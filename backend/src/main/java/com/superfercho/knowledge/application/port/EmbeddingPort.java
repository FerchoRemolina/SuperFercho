package com.superfercho.knowledge.application.port;

import com.superfercho.knowledge.application.dto.EmbeddingVector;
import java.util.List;

public interface EmbeddingPort {

    List<EmbeddingVector> embed(List<String> texts);
}
