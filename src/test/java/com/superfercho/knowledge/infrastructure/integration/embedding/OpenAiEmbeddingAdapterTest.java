package com.superfercho.knowledge.infrastructure.integration.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.superfercho.knowledge.application.dto.EmbeddingVector;
import com.superfercho.knowledge.application.exception.KnowledgeProcessingException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiEmbeddingAdapterTest {

    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final OpenAiEmbeddingProperties PROPERTIES =
            new OpenAiEmbeddingProperties("test-openai-key", EMBEDDINGS_URL, "text-embedding-3-small");

    private MockRestServiceServer server;
    private OpenAiEmbeddingAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new OpenAiEmbeddingAdapter(builder.build(), PROPERTIES);
    }

    @Test
    void shouldMapRequestAndSingleEmbeddingPreservingDimension() {
        server.expect(requestTo(EMBEDDINGS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-openai-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(requestJson("hola"), true))
                .andRespond(withSuccess(responseJson(dataJson(0, 0.25f)), MediaType.APPLICATION_JSON));

        List<EmbeddingVector> embeddings = adapter.embed(List.of("hola"));

        server.verify();
        assertEquals(1, embeddings.size());
        assertEquals(1536, embeddings.get(0).values().length);
        assertEquals(0.25f, embeddings.get(0).values()[0]);
    }

    @Test
    void shouldPreserveInputOrderForMultipleTexts() {
        server.expect(requestTo(EMBEDDINGS_URL))
                .andExpect(content().json(requestJson("uno", "dos"), true))
                .andRespond(withSuccess(
                        responseJson(dataJson(1, 0.2f) + "," + dataJson(0, 0.1f)), MediaType.APPLICATION_JSON));

        List<EmbeddingVector> embeddings = adapter.embed(List.of("uno", "dos"));

        server.verify();
        assertEquals(2, embeddings.size());
        assertEquals(0.1f, embeddings.get(0).values()[0]);
        assertEquals(0.2f, embeddings.get(1).values()[0]);
    }

    @Test
    void shouldRejectEmptyProviderResponse() {
        server.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(KnowledgeProcessingException.class, () -> adapter.embed(List.of("hola")));
    }

    @Test
    void shouldRejectIncorrectVectorCount() {
        server.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess(responseJson(dataJson(0, 0.1f)), MediaType.APPLICATION_JSON));

        assertThrows(KnowledgeProcessingException.class, () -> adapter.embed(List.of("uno", "dos")));
    }

    @Test
    void shouldRejectNullVector() {
        server.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess("{\"data\":[{\"index\":0,\"embedding\":null}]}", MediaType.APPLICATION_JSON));

        assertThrows(KnowledgeProcessingException.class, () -> adapter.embed(List.of("hola")));
    }

    @Test
    void shouldRejectIncorrectDimension() {
        server.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess("{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2]}]}", MediaType.APPLICATION_JSON));

        assertThrows(KnowledgeProcessingException.class, () -> adapter.embed(List.of("hola")));
    }

    @Test
    void shouldTranslateProviderHttpError() {
        server.expect(requestTo(EMBEDDINGS_URL)).andRespond(withServerError());

        KnowledgeProcessingException exception =
                assertThrows(KnowledgeProcessingException.class, () -> adapter.embed(List.of("hola")));
        assertEquals("embedding provider request failed", exception.getMessage());
    }

    @Test
    void shouldRejectMissingApiKeyWithoutCallingProvider() {
        OpenAiEmbeddingAdapter withoutKey = new OpenAiEmbeddingAdapter(
                RestClient.builder().build(),
                new OpenAiEmbeddingProperties("  ", EMBEDDINGS_URL, "text-embedding-3-small"));

        KnowledgeProcessingException exception =
                assertThrows(KnowledgeProcessingException.class, () -> withoutKey.embed(List.of("hola")));
        assertEquals("OpenAI API key is not configured", exception.getMessage());
    }

    private static String requestJson(String... inputs) {
        StringBuilder json = new StringBuilder("{\"model\":\"text-embedding-3-small\",\"input\":[");
        for (int index = 0; index < inputs.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(inputs[index]).append('"');
        }
        json.append("],\"dimensions\":1536}");
        return json.toString();
    }

    private static String responseJson(String dataItems) {
        return "{\"data\":[" + dataItems + "]}";
    }

    private static String dataJson(int index, float firstValue) {
        StringBuilder values = new StringBuilder();
        values.append(firstValue);
        for (int offset = 1; offset < 1536; offset++) {
            values.append(",0.0");
        }
        return "{\"index\":" + index + ",\"embedding\":[" + values + "]}";
    }
}
