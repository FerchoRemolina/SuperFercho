package com.superfercho.knowledge.infrastructure.integration.embedding;

import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import com.superfercho.knowledge.application.port.EmbeddingPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    static final int EMBEDDING_DIMENSIONS = 1536;

    private final RestClient restClient;
    private final OpenAiEmbeddingProperties properties;

    public OpenAiEmbeddingAdapter(RestClient restClient, OpenAiEmbeddingProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<EmbeddingVector> embed(List<String> texts) {
        if (texts == null) {
            throw new KnowledgeProcessingException("embedding texts cannot be null");
        }
        if (!properties.hasApiKey()) {
            throw new KnowledgeProcessingException("OpenAI API key is not configured");
        }
        if (texts.isEmpty()) {
            return List.of();
        }
        OpenAiEmbeddingResponse response;
        try {
            response = restClient
                    .post()
                    .uri(properties.embeddingsUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new OpenAiEmbeddingRequest(properties.model(), List.copyOf(texts), EMBEDDING_DIMENSIONS))
                    .retrieve()
                    .body(OpenAiEmbeddingResponse.class);
        } catch (RestClientException exception) {
            throw new KnowledgeProcessingException("embedding provider request failed");
        }
        return toVectors(response, texts.size());
    }

    private List<EmbeddingVector> toVectors(OpenAiEmbeddingResponse response, int expectedSize) {
        if (response == null || response.data() == null) {
            throw new KnowledgeProcessingException("embedding provider returned an invalid result");
        }
        if (response.data().size() != expectedSize) {
            throw new KnowledgeProcessingException("embedding provider returned an invalid result");
        }
        List<OpenAiEmbeddingData> ordered = new ArrayList<>(response.data());
        ordered.sort(Comparator.comparingInt(OpenAiEmbeddingData::index));
        List<EmbeddingVector> vectors = new ArrayList<>(expectedSize);
        for (int index = 0; index < ordered.size(); index++) {
            OpenAiEmbeddingData item = ordered.get(index);
            if (item == null || item.embedding() == null) {
                throw new KnowledgeProcessingException("embedding provider returned an invalid result");
            }
            if (item.index() != index) {
                throw new KnowledgeProcessingException("embedding provider returned an invalid result");
            }
            if (item.embedding().length != EMBEDDING_DIMENSIONS) {
                throw new KnowledgeProcessingException("embedding provider returned an invalid result");
            }
            vectors.add(new EmbeddingVector(item.embedding()));
        }
        return List.copyOf(vectors);
    }
}
