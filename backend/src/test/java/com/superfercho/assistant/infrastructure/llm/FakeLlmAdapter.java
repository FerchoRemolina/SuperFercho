package com.superfercho.assistant.infrastructure.llm;

import com.superfercho.assistant.application.dto.llm.LlmRequest;
import com.superfercho.assistant.application.dto.llm.LlmResponse;
import com.superfercho.assistant.application.port.out.LLMPort;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class FakeLlmAdapter implements LLMPort {

    private final Deque<LlmResponse> scripted = new ArrayDeque<>();
    private final List<LlmRequest> requests = new ArrayList<>();

    public FakeLlmAdapter enqueue(LlmResponse response) {
        scripted.addLast(response);
        return this;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        requests.add(request);
        if (scripted.isEmpty()) {
            throw new IllegalStateException("no scripted LLM response remaining");
        }
        return scripted.removeFirst();
    }

    public List<LlmRequest> requests() {
        return List.copyOf(requests);
    }
}
