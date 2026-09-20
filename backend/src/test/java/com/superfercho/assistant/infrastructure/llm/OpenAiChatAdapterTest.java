package com.superfercho.assistant.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superfercho.assistant.application.dto.llm.LlmMessage;
import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.dto.llm.LlmToolDefinition;
import com.superfercho.assistant.application.exception.LlmProviderException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiChatAdapterTest {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "test-openai-key";
    private static final OpenAiChatProperties PROPERTIES = new OpenAiChatProperties(
            API_KEY, CHAT_URL, "gpt-4o-mini", Duration.ofSeconds(5), Duration.ofSeconds(60));

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;
    private OpenAiChatAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new OpenAiChatAdapter(builder.build(), PROPERTIES, objectMapper);
    }

    @Test
    void shouldMapUserMessageAndToolsToProviderRequest() {
        server.expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(userSearchRequestJson(), true))
                .andRespond(withSuccess(textResponseJson("Hay leche disponible."), MediaType.APPLICATION_JSON));

        LlmResponse response = adapter.complete(new LlmRequest(
                List.of(LlmMessage.user("busca leche")),
                List.of(new LlmToolDefinition(
                        "search_products",
                        "Search products",
                        List.of(new LlmToolDefinition.LlmToolParameter(
                                "query", "string", true, "Search text"))))));

        server.verify();
        assertEquals("Hay leche disponible.", response.text());
        assertTrue(response.toolCalls().isEmpty());
    }

    @Test
    void shouldMapTextualProviderResponse() {
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(textResponseJson("Hola"), MediaType.APPLICATION_JSON));

        LlmResponse response = adapter.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of()));

        assertEquals("Hola", response.text());
        assertFalse(response.hasToolCalls());
    }

    @Test
    void shouldMapSingleToolCallFromProvider() {
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(toolCallsResponseJson(toolCallJson("c1", "get_cart", "{}")), MediaType.APPLICATION_JSON));

        LlmResponse response = adapter.complete(new LlmRequest(List.of(LlmMessage.user("ver carrito")), List.of()));

        assertTrue(response.hasToolCalls());
        assertEquals(1, response.toolCalls().size());
        assertEquals("c1", response.toolCalls().get(0).id());
        assertEquals("get_cart", response.toolCalls().get(0).name());
        assertEquals(Map.of(), response.toolCalls().get(0).arguments());
    }

    @Test
    void shouldMapMultipleToolCallsFromProvider() {
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(
                        toolCallsResponseJson(
                                toolCallJson("c1", "get_cart", "{}")
                                        + ","
                                        + toolCallJson("c2", "search_knowledge", "{\"query\":\"pollo\"}")),
                        MediaType.APPLICATION_JSON));

        LlmResponse response = adapter.complete(new LlmRequest(List.of(LlmMessage.user("carrito y receta")), List.of()));

        assertEquals(2, response.toolCalls().size());
        assertEquals("get_cart", response.toolCalls().get(0).name());
        assertEquals("search_knowledge", response.toolCalls().get(1).name());
        assertEquals("pollo", response.toolCalls().get(1).arguments().get("query"));
    }

    @Test
    void shouldMapToolResultMessagesToProviderRequest() {
        server.expect(requestTo(CHAT_URL))
                .andExpect(content().json(toolResultRequestJson(), true))
                .andRespond(withSuccess(textResponseJson("Tu carrito está vacío."), MediaType.APPLICATION_JSON));

        LlmResponse response = adapter.complete(new LlmRequest(
                List.of(
                        LlmMessage.user("ver carrito"),
                        LlmMessage.assistantToolCalls(List.of(new LlmMessage.LlmToolCall("c1", "get_cart", Map.of()))),
                        LlmMessage.tool("c1", "get_cart", "cart is empty")),
                List.of()));

        server.verify();
        assertEquals("Tu carrito está vacío.", response.text());
    }

    @Test
    void shouldTranslateHttpServerError() {
        server.expect(requestTo(CHAT_URL)).andRespond(withServerError());

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> adapter.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of())));
        assertEquals("llm provider request failed", exception.getMessage());
        assertNull(exception.getCause());
        assertFalse(exception.getMessage().contains(API_KEY));
    }

    @Test
    void shouldTranslateAuthenticationErrorWithoutLeakingSecrets() {
        server.expect(requestTo(CHAT_URL))
                .andRespond(withUnauthorizedRequest().body("Incorrect API key provided: sk-secret"));

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> adapter.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of())));
        assertEquals("llm provider authentication failed", exception.getMessage());
        assertNull(exception.getCause());
        assertFalse(exception.getMessage().contains("sk-secret"));
        assertFalse(exception.getMessage().contains(API_KEY));
        assertFalse(exception.toString().contains("sk-secret"));
        assertFalse(exception.toString().contains(API_KEY));
    }

    @Test
    void shouldTranslateRateLimit() {
        server.expect(requestTo(CHAT_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> adapter.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of())));
        assertEquals("llm provider rate limit exceeded", exception.getMessage());
    }

    @Test
    void shouldTranslateTimeout() {
        RestClient restClient = RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException("Read timed out");
                })
                .build();
        OpenAiChatAdapter timingOut = new OpenAiChatAdapter(restClient, PROPERTIES, objectMapper);

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> timingOut.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of())));
        assertEquals("llm provider request timed out", exception.getMessage());
        assertFalse(exception.getMessage().contains(API_KEY));
    }

    @Test
    void shouldRejectInvalidJsonResponse() {
        server.expect(requestTo(CHAT_URL)).andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> adapter.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of())));
        assertEquals("llm provider returned an invalid result", exception.getMessage());
    }

    @Test
    void shouldRejectMalformedToolCallArguments() {
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(
                        toolCallsResponseJson(toolCallJson("c1", "get_cart", "not-json")),
                        MediaType.APPLICATION_JSON));

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> adapter.complete(new LlmRequest(List.of(LlmMessage.user("ver carrito")), List.of())));
        assertEquals("llm provider returned an invalid result", exception.getMessage());
    }

    @Test
    void shouldRejectToolCallArgumentsContainingNullValues() {
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(
                        toolCallsResponseJson(toolCallJson("c1", "get_cart", "{\"productId\":null}")),
                        MediaType.APPLICATION_JSON));

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> adapter.complete(new LlmRequest(List.of(LlmMessage.user("ver carrito")), List.of())));
        assertEquals("llm provider returned an invalid result", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldRejectMissingApiKeyWithoutCallingProvider() {
        OpenAiChatAdapter withoutKey = new OpenAiChatAdapter(
                RestClient.builder().build(),
                new OpenAiChatProperties("  ", CHAT_URL, "gpt-4o-mini", Duration.ofSeconds(5), Duration.ofSeconds(60)),
                objectMapper);

        LlmProviderException exception = assertThrows(
                LlmProviderException.class,
                () -> withoutKey.complete(new LlmRequest(List.of(LlmMessage.user("hola")), List.of())));
        assertEquals("OpenAI API key is not configured", exception.getMessage());
    }

    private static String userSearchRequestJson() {
        return """
                {
                  "model": "gpt-4o-mini",
                  "messages": [{"role":"user","content":"busca leche"}],
                  "tools": [{
                    "type": "function",
                    "function": {
                      "name": "search_products",
                      "description": "Search products",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "query": {"type":"string","description":"Search text"}
                        },
                        "required": ["query"]
                      }
                    }
                  }]
                }
                """;
    }

    private static String toolResultRequestJson() {
        return """
                {
                  "model": "gpt-4o-mini",
                  "messages": [
                    {"role":"user","content":"ver carrito"},
                    {"role":"assistant","tool_calls":[{"id":"c1","type":"function","function":{"name":"get_cart","arguments":"{}"}}]},
                    {"role":"tool","content":"cart is empty","tool_call_id":"c1","name":"get_cart"}
                  ]
                }
                """;
    }

    private static String textResponseJson(String text) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + text + "\"}}]}";
    }

    private static String toolCallsResponseJson(String toolCalls) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + toolCalls
                + "]}}]}";
    }

    private static String toolCallJson(String id, String name, String argumentsJson) {
        String escaped = argumentsJson.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"id\":\""
                + id
                + "\",\"type\":\"function\",\"function\":{\"name\":\""
                + name
                + "\",\"arguments\":\""
                + escaped
                + "\"}}";
    }
}
