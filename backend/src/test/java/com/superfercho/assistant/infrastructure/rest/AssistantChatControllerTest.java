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
import com.superfercho.assistant.application.dto.chat.PackageProposalOptionView;
import com.superfercho.assistant.application.dto.chat.PackageProposalView;
import com.superfercho.assistant.application.dto.chat.PackageSelection;
import com.superfercho.assistant.application.exception.FerchoPausedException;
import com.superfercho.assistant.application.exception.InvalidPackageSelectionException;
import com.superfercho.catalog.domain.model.PresentationUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Hay leche disponible.", false, null, null, false));

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
                .andExpect(jsonPath("$.confirmationType").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.authenticationRequired").value(false));

        verify(chatUseCase).execute(new ChatCommand(null, "¿Qué productos tienen?", null));
    }

    @Test
    void shouldMapAuthenticationRequiredFlag() throws Exception {
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Inicia sesion para usar tu carrito.", false, null, null, true));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "muestrame mi carrito"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationRequired").value(true))
                .andExpect(jsonPath("$.assistantMessage").value("Inicia sesion para usar tu carrito."));
    }

    @Test
    void shouldForwardExistingConversationId() throws Exception {
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Continuemos.", false, null, null, false));

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
                .thenReturn(new ChatResponse(CONVERSATION_ID, "Checkout confirmed.", false, null, null, false));

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
                        CONVERSATION_ID, "¿Confirmas la compra?", true, CONFIRMATION_TOKEN, SensitiveActionType.CHECKOUT, false));

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
    void shouldMapPauseWithReadableUtf8Detail() throws Exception {
        Instant pausedUntil = Instant.parse("2026-01-01T00:30:00Z");
        when(chatUseCase.execute(any()))
                .thenThrow(new FerchoPausedException(
                        "FERCHO_PAUSED",
                        "Fercho est\u00e1 tomando un descanso. Int\u00e9ntalo m\u00e1s tarde.",
                        1800,
                        pausedUntil));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hola"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FERCHO_PAUSED"))
                .andExpect(jsonPath("$.detail").value("Fercho est\u00e1 tomando un descanso. Int\u00e9ntalo m\u00e1s tarde."))
                .andExpect(jsonPath("$.detail").value(Matchers.not(Matchers.containsString("\u00c3"))))
                .andExpect(jsonPath("$.remainingSeconds").value(1800))
                .andExpect(jsonPath("$.pausedUntil").value(pausedUntil.toString()));
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
    void shouldForwardPackageSelectionWithoutInvokingConfirmation() throws Exception {
        UUID proposalId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(
                        CONVERSATION_ID,
                        "Agregué 2 paquete(s) de Arroz (1 KG) al carrito.",
                        false,
                        null,
                        null,
                        false,
                        null));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "packageSelection": {
                                    "proposalId": "%s",
                                    "packageCount": 2
                                  }
                                }
                                """.formatted(CONVERSATION_ID, proposalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageProposal").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.awaitingConfirmation").value(false));

        verify(chatUseCase)
                .execute(new ChatCommand(CONVERSATION_ID, null, null, new PackageSelection(proposalId, 2)));
    }

    @Test
    void shouldRejectCombinedConfirmationAndPackageSelection() throws Exception {
        when(chatUseCase.execute(any()))
                .thenThrow(new InvalidChatRequestException("a turn cannot combine confirmation and packageSelection"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "confirmation": { "token": "%s" },
                                  "packageSelection": {
                                    "proposalId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "packageCount": 2
                                  }
                                }
                                """.formatted(CONVERSATION_ID, CONFIRMATION_TOKEN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CHAT_REQUEST"));
    }

    @Test
    void shouldMapInvalidPackageSelectionWithoutLeakingOwnership() throws Exception {
        when(chatUseCase.execute(any()))
                .thenThrow(new InvalidPackageSelectionException("package selection does not belong to the authenticated user"));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "packageSelection": {
                                    "proposalId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    "packageCount": 2
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PACKAGE_SELECTION"))
                .andExpect(jsonPath("$.detail").value("Invalid package selection"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("does not belong"))));
    }

    @Test
    void shouldExposePackageProposalChipsOnTextResponses() throws Exception {
        UUID proposalId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID productId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(chatUseCase.execute(any()))
                .thenReturn(new ChatResponse(
                        CONVERSATION_ID,
                        "Elige paquetes",
                        false,
                        null,
                        null,
                        false,
                        new PackageProposalView(
                                proposalId,
                                productId,
                                "Arroz",
                                new BigDecimal("1.000"),
                                PresentationUnit.KG,
                                new BigDecimal("2.300"),
                                PresentationUnit.KG,
                                List.of(new PackageProposalOptionView(
                                        2, new BigDecimal("2000.000"), PresentationUnit.G)))));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "quiero 2.3 kg de arroz"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageProposal.proposalId").value(proposalId.toString()))
                .andExpect(jsonPath("$.packageProposal.options[0].packageCount").value(2));
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
