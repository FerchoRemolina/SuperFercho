package com.superfercho.assistant.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.chat.ExplicitConfirmation;
import com.superfercho.assistant.application.exception.ConversationNotFoundException;
import com.superfercho.assistant.application.exception.InvalidChatRequestException;
import com.superfercho.assistant.application.exception.InvalidConfirmationException;
import com.superfercho.assistant.application.exception.InvalidToolArgumentsException;
import com.superfercho.assistant.application.exception.LlmProviderException;
import com.superfercho.assistant.application.exception.ToolNotAllowedException;
import com.superfercho.assistant.application.port.in.ChatUseCase;
import com.superfercho.platform.error.ApiExceptionHandler;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AssistantChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AssistantExceptionHandler.class, ApiExceptionHandler.class})
class AssistantChatControllerTest {

    private static final UUID CONVERSATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String CONFIRMATION_TOKEN = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatUseCase chatUseCase;

    @Test
    void shouldChatWithoutConversationId() throws Exception {
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Hay leche disponible.", false, null, null));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": null,
                                  "message": "¿Qué productos tienen?",
                                  "confirmation": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID.toString()))
                .andExpect(jsonPath("$.assistantMessage").value("Hay leche disponible."))
                .andExpect(jsonPath("$.awaitingConfirmation").value(false))
                .andExpect(jsonPath("$.confirmationToken").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.confirmationType").value(Matchers.nullValue()));

        verify(chatUseCase).execute(new ChatCommand(null, "¿Qué productos tienen?", null));
    }

    @Test
    void shouldForwardExistingConversationId() throws Exception {
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Continuemos.", false, null, null));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "message": "sigue",
                                  "confirmation": null
                                }
                                """.formatted(CONVERSATION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID.toString()));

        verify(chatUseCase).execute(new ChatCommand(CONVERSATION_ID, "sigue", null));
    }

    @Test
    void shouldForwardConfirmationToken() throws Exception {
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Checkout confirmed.", false, null, null));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "message": null,
                                  "confirmation": {
                                    "token": "%s"
                                  }
                                }
                                """.formatted(CONVERSATION_ID, CONFIRMATION_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage").value("Checkout confirmed."))
                .andExpect(jsonPath("$.awaitingConfirmation").value(false));

        verify(chatUseCase)
                .execute(new ChatCommand(CONVERSATION_ID, null, new ExplicitConfirmation(CONFIRMATION_TOKEN)));
    }

    @Test
    void shouldForwardConfirmationAndMessageWithoutChoosingPriority() throws Exception {
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(
                        CONVERSATION_ID, "¿Confirmas la compra?", true, CONFIRMATION_TOKEN, SensitiveActionType.CHECKOUT));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "message": "confirmo",
                                  "confirmation": {
                                    "token": "%s"
                                  }
                                }
                                """.formatted(CONVERSATION_ID, CONFIRMATION_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awaitingConfirmation").value(true))
                .andExpect(jsonPath("$.confirmationToken").value(CONFIRMATION_TOKEN))
                .andExpect(jsonPath("$.confirmationType").value("CHECKOUT"));

        verify(chatUseCase)
                .execute(new ChatCommand(CONVERSATION_ID, "confirmo", new ExplicitConfirmation(CONFIRMATION_TOKEN)));
    }

    @Test
    void shouldMapInvalidChatRequestToBadRequest() throws Exception {
        when(chatUseCase.execute(any())).thenThrow(new InvalidChatRequestException("message cannot be blank"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": null,
                                  "message": "   ",
                                  "confirmation": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("INVALID_CHAT_REQUEST"));
    }

    @Test
    void shouldMapMissingConversationToNotFound() throws Exception {
        when(chatUseCase.execute(any())).thenThrow(new ConversationNotFoundException());

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "message": "hola",
                                  "confirmation": null
                                }
                                """.formatted(CONVERSATION_ID)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Conversation not found"))
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    void shouldMapInvalidConfirmationWithoutLeakingOwnership() throws Exception {
        when(chatUseCase.execute(any()))
                .thenThrow(new InvalidConfirmationException("confirmation does not belong to the authenticated user"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "message": null,
                                  "confirmation": {
                                    "token": "%s"
                                  }
                                }
                                """.formatted(CONVERSATION_ID, CONFIRMATION_TOKEN)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid confirmation"))
                .andExpect(jsonPath("$.code").value("INVALID_CONFIRMATION"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("does not belong"))));
    }

    @Test
    void shouldMapLlmFailureWithoutExposingProviderSecrets() throws Exception {
        when(chatUseCase.execute(any()))
                .thenThrow(new LlmProviderException("OpenAI API key is not configured"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hola"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("LLM provider request failed"))
                .andExpect(jsonPath("$.code").value("LLM_PROVIDER_FAILED"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("OpenAI"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("API key"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Authorization"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("OpenAI API key is not configured"))));
    }

    @Test
    void shouldMapInvalidToolArgumentsToBadRequest() throws Exception {
        when(chatUseCase.execute(any())).thenThrow(new InvalidToolArgumentsException("productId is required"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "agrega leche"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TOOL_ARGUMENTS"));
    }

    @Test
    void shouldMapToolNotAllowedToBadRequest() throws Exception {
        when(chatUseCase.execute(any())).thenThrow(new ToolNotAllowedException("secret_tool"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "usa una tool prohibida"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("TOOL_NOT_ALLOWED"));
    }
}
