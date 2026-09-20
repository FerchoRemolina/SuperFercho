package com.superfercho.assistant.application.port.out;

import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;

public interface LLMPort {

    LlmResponse complete(LlmRequest request);
}
