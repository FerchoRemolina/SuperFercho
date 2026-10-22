package com.superfercho.identity.infrastructure.rest.dto;

/** Cuerpo de PUT /customers/me. La identidad del cliente llega por JWT, no por el cuerpo. */
public record UpdateCustomerProfileRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String documentType,
        String documentNumber) {}
