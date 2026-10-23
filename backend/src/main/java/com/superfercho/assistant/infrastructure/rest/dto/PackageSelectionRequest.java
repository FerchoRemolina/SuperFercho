package com.superfercho.assistant.infrastructure.rest.dto;

import java.util.UUID;

public record PackageSelectionRequest(UUID proposalId, Integer packageCount) {}
