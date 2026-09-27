package com.superfercho.identity.infrastructure.rest.dto;

public record ResetPasswordRequest(String token, String newPassword, String confirmPassword) {}
