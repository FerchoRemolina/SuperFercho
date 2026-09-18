package com.superfercho.knowledge.infrastructure.integration.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiEmbeddingData(int index, float[] embedding) {}
