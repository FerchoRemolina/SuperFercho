package com.superfercho.identity.application.dto;

public record RequestPasswordRecoveryCommand(String email, String clientIp) {}
