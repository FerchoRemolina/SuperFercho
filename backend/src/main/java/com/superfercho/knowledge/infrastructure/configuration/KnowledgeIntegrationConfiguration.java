package com.superfercho.knowledge.infrastructure.configuration;

import com.superfercho.knowledge.application.port.EmbeddingPort;
import com.superfercho.knowledge.infrastructure.integration.embedding.OpenAiEmbeddingAdapter;
import com.superfercho.knowledge.infrastructure.integration.embedding.OpenAiEmbeddingProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiEmbeddingProperties.class)
public class KnowledgeIntegrationConfiguration {

    @Bean
    @Profile("!test")
    EmbeddingPort knowledgeEmbeddingPort(
            RestClient.Builder restClientBuilder, OpenAiEmbeddingProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return new OpenAiEmbeddingAdapter(restClientBuilder.requestFactory(requestFactory).build(), properties);
    }
}
