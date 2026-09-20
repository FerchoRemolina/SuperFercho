package com.superfercho.knowledge.infrastructure.integration.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiEmbeddingResponse(List<OpenAiEmbeddingData> data) {}
