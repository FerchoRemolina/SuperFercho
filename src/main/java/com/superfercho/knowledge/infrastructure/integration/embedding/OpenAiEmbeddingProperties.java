package com.superfercho.knowledge.infrastructure.integration.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "superfercho.knowledge.openai")
public record OpenAiEmbeddingProperties(String apiKey, String embeddingsUrl, String model) {

    public OpenAiEmbeddingProperties {
        apiKey = apiKey == null ? "" : apiKey;
        embeddingsUrl = embeddingsUrl == null || embeddingsUrl.isBlank()
                ? "https://api.openai.com/v1/embeddings"
                : embeddingsUrl;
        model = model == null || model.isBlank() ? "text-embedding-3-small" : model;
    }

    boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}
