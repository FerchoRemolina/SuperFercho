package com.superfercho.knowledge.application.dto;

import java.util.List;

public record KnowledgeSearchResult(List<KnowledgeSearchHit> hits) {

    public KnowledgeSearchResult {
        hits = List.copyOf(hits);
    }
}
