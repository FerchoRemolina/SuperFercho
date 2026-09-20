package com.superfercho.knowledge.application.dto;

import java.util.List;
import java.util.UUID;

public record ChunkResult(UUID id, int position, String text, boolean embedded) {}
