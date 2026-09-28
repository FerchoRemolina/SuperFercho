package com.superfercho.identity.infrastructure.rest.dto;

public record RequestPasswordRecoveryByDocumentRequest(String documentType, String documentNumber) {}
