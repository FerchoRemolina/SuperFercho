package com.superfercho.identity.application.dto;

public record AuthenticateUserCommand(String email, String password) {
}
