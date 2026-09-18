package com.superfercho.knowledge.infrastructure.integration.embedding;

import java.util.List;

record OpenAiEmbeddingRequest(String model, List<String> input, int dimensions) {}
