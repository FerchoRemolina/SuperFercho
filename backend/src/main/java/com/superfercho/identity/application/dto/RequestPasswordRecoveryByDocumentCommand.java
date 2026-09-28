package com.superfercho.identity.application.dto;

public record RequestPasswordRecoveryByDocumentCommand(
        String documentType, String documentNumber, String clientIp) {}
