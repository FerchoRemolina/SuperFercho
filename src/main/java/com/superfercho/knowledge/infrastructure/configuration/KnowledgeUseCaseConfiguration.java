package com.superfercho.knowledge.infrastructure.configuration;

import com.superfercho.knowledge.application.port.ClockPort;
import com.superfercho.knowledge.application.port.DocumentChunkerPort;
import com.superfercho.knowledge.application.port.EmbeddingPort;
import com.superfercho.knowledge.application.port.KnowledgeDocumentRepository;
import com.superfercho.knowledge.application.port.KnowledgeVectorStorePort;
import com.superfercho.knowledge.application.usecase.CreateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.DeactivateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.GetDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ListDocumentsUseCase;
import com.superfercho.knowledge.application.usecase.ProcessDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ReactivateDocumentUseCase;
import com.superfercho.knowledge.application.usecase.ReplaceDocumentContentUseCase;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import com.superfercho.knowledge.infrastructure.clock.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KnowledgeUseCaseConfiguration {

    @Bean
    ClockPort knowledgeClockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    CreateDocumentUseCase createDocumentUseCase(
            KnowledgeDocumentRepository documentRepository, ClockPort knowledgeClockPort) {
        return new CreateDocumentUseCase(documentRepository, knowledgeClockPort);
    }

    @Bean
    GetDocumentUseCase getDocumentUseCase(KnowledgeDocumentRepository documentRepository) {
        return new GetDocumentUseCase(documentRepository);
    }

    @Bean
    ListDocumentsUseCase listDocumentsUseCase(KnowledgeDocumentRepository documentRepository) {
        return new ListDocumentsUseCase(documentRepository);
    }

    @Bean
    ReplaceDocumentContentUseCase replaceDocumentContentUseCase(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeVectorStorePort vectorStore,
            ClockPort knowledgeClockPort) {
        return new ReplaceDocumentContentUseCase(documentRepository, vectorStore, knowledgeClockPort);
    }

    @Bean
    ProcessDocumentUseCase processDocumentUseCase(
            KnowledgeDocumentRepository documentRepository,
            DocumentChunkerPort chunker,
            EmbeddingPort embeddingPort,
            KnowledgeVectorStorePort vectorStore,
            ClockPort knowledgeClockPort) {
        return new ProcessDocumentUseCase(
                documentRepository, chunker, embeddingPort, vectorStore, knowledgeClockPort);
    }

    @Bean
    SearchKnowledgeUseCase searchKnowledgeUseCase(
            EmbeddingPort embeddingPort, KnowledgeVectorStorePort vectorStore) {
        return new SearchKnowledgeUseCase(embeddingPort, vectorStore);
    }

    @Bean
    DeactivateDocumentUseCase deactivateDocumentUseCase(
            KnowledgeDocumentRepository documentRepository, ClockPort knowledgeClockPort) {
        return new DeactivateDocumentUseCase(documentRepository, knowledgeClockPort);
    }

    @Bean
    ReactivateDocumentUseCase reactivateDocumentUseCase(
            KnowledgeDocumentRepository documentRepository, ClockPort knowledgeClockPort) {
        return new ReactivateDocumentUseCase(documentRepository, knowledgeClockPort);
    }
}
