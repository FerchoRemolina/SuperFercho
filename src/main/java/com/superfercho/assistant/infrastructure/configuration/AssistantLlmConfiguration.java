package com.superfercho.assistant.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superfercho.assistant.application.port.out.LLMPort;
import com.superfercho.assistant.infrastructure.llm.OpenAiChatAdapter;
import com.superfercho.assistant.infrastructure.llm.OpenAiChatProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(OpenAiChatProperties.class)
public class AssistantLlmConfiguration {

    @Bean
    LLMPort assistantLlmPort(
            RestClient.Builder restClientBuilder, OpenAiChatProperties properties, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return new OpenAiChatAdapter(
                restClientBuilder.clone().requestFactory(requestFactory).build(), properties, objectMapper);
    }
}
