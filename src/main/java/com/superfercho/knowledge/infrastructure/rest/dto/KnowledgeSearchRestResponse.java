package com.superfercho.knowledge.infrastructure.rest.dto;

import com.superfercho.knowledge.application.dto.KnowledgeSearchResult;
import java.util.List;

public record KnowledgeSearchRestResponse(List<KnowledgeSearchHitRestResponse> hits) {

    public static KnowledgeSearchRestResponse from(KnowledgeSearchResult result) {
        return new KnowledgeSearchRestResponse(
                result.hits().stream().map(KnowledgeSearchHitRestResponse::from).toList());
    }
}
