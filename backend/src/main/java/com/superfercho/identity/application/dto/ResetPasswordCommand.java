package com.superfercho.identity.application.dto;

public record ResetPasswordCommand(String token, String newPassword, String confirmPassword) {}
