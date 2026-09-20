package com.superfercho.knowledge.infrastructure.rest.dto;

import com.superfercho.knowledge.application.dto.ChunkResult;
import java.util.UUID;

public record ChunkRestResponse(UUID id, int position, String text, boolean embedded) {

    public static ChunkRestResponse from(ChunkResult result) {
        return new ChunkRestResponse(result.id(), result.position(), result.text(), result.embedded());
    }
}
