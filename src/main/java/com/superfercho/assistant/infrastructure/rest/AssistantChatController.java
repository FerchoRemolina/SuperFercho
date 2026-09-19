package com.superfercho.assistant.infrastructure.rest;

import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ExplicitConfirmation;
import com.superfercho.assistant.application.port.in.ChatUseCase;
import com.superfercho.assistant.infrastructure.rest.dto.ChatRequest;
import com.superfercho.assistant.infrastructure.rest.dto.ChatRestResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/assistant")
public class AssistantChatController {

    private final ChatUseCase chatUseCase;

    public AssistantChatController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping("/chat")
    public ChatRestResponse chat(@RequestBody ChatRequest request) {
        ExplicitConfirmation confirmation = request.confirmation() == null
                ? null
                : new ExplicitConfirmation(request.confirmation().token());
        return ChatRestResponse.from(
                chatUseCase.execute(new ChatCommand(request.conversationId(), request.message(), confirmation)));
    }
}
