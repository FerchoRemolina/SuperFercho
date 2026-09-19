package com.superfercho.assistant.application.port.in;

import com.superfercho.assistant.application.dto.chat.ChatCommand;
import com.superfercho.assistant.application.dto.chat.ChatResponse;

public interface ChatUseCase {

    ChatResponse execute(ChatCommand command);
}
