package com.superfercho.assistant.infrastructure.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "superfercho.assistant.openai")
public record OpenAiChatProperties(
        String apiKey, String chatUrl, String model, Duration connectTimeout, Duration readTimeout) {

    public OpenAiChatProperties {
        apiKey = apiKey == null ? "" : apiKey;
        chatUrl = chatUrl == null || chatUrl.isBlank()
                ? "https://api.openai.com/v1/chat/completions"
                : chatUrl;
        model = model == null || model.isBlank() ? "gpt-4o-mini" : model;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
    }

    boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}
