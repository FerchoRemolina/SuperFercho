package com.superfercho.identity.infrastructure.rest.dto;

public record AuthenticateUserRequest(String email, String password) {
}
