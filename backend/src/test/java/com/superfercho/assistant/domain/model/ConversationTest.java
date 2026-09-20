package com.superfercho.assistant.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.assistant.domain.exception.InvalidConversationException;
import com.superfercho.assistant.domain.exception.InvalidMessageException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationTest {

    private static final Instant NOW = Instant.parse("2026-09-18T12:00:00Z");
    private static final UUID CONVERSATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldStartEmptyConversation() {
        Conversation conversation = Conversation.start(CONVERSATION_ID, USER_ID, NOW);

        assertEquals(CONVERSATION_ID, conversation.id());
        assertEquals(USER_ID, conversation.userId());
        assertEquals(List.of(), conversation.messages());
    }

    @Test
    void shouldAppendUserAndAssistantMessages() {
        Conversation conversation = Conversation.start(CONVERSATION_ID, USER_ID, NOW)
                .append(Message.user(UUID.randomUUID(), "hola", NOW), NOW)
                .append(Message.assistant(UUID.randomUUID(), "hola, ¿en qué ayudo?", NOW), NOW);

        assertEquals(2, conversation.messages().size());
        assertEquals(MessageRole.USER, conversation.messages().get(0).role());
        assertEquals(MessageRole.ASSISTANT, conversation.messages().get(1).role());
    }

    @Test
    void shouldRejectBlankUserContent() {
        assertThrows(
                InvalidMessageException.class, () -> Message.user(UUID.randomUUID(), "  ", NOW));
    }

    @Test
    void shouldKeepToolCallDataOnAssistantMessage() {
        Message message = Message.assistantToolCalls(
                UUID.randomUUID(),
                List.of(new MessageToolCall("call-1", "get_cart", Map.of())),
                NOW);

        assertEquals(1, message.toolCalls().size());
        assertEquals("get_cart", message.toolCalls().get(0).name());
    }

    @Test
    void shouldRejectNullConversationId() {
        assertThrows(InvalidConversationException.class, () -> Conversation.start(null, USER_ID, NOW));
    }
}
