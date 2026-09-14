package com.superfercho.identity.application.dto;

public record RegisterCustomerCommand(
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        String phone,
        String password) {
}
