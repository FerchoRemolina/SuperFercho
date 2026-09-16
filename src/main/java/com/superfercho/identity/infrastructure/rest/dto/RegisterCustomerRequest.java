package com.superfercho.identity.infrastructure.rest.dto;

public record RegisterCustomerRequest(
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        String phone,
        String password) {
}
