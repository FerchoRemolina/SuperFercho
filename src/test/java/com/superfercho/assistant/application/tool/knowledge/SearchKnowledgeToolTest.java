package com.superfercho.assistant.application.tool.knowledge;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.knowledge.application.dto.KnowledgeSearchResult;
import com.superfercho.knowledge.application.dto.SearchKnowledgeCommand;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchKnowledgeToolTest {

    @Mock
    private SearchKnowledgeUseCase searchKnowledgeUseCase;

    @Test
    void delegatesToSearchKnowledgeUseCase() {
        when(searchKnowledgeUseCase.execute(new SearchKnowledgeCommand("pollo", 5)))
                .thenReturn(new KnowledgeSearchResult(List.of()));

        assertTrue(new SearchKnowledgeTool(searchKnowledgeUseCase)
                .execute(ToolArguments.of(Map.of("query", "pollo")))
                .success());
        verify(searchKnowledgeUseCase).execute(new SearchKnowledgeCommand("pollo", 5));
    }
}
