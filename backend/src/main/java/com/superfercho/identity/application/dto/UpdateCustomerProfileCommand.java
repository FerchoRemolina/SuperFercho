package com.superfercho.identity.application.dto;

/** Datos personales editables del CUSTOMER autenticado. La identidad llega por JWT. */
public record UpdateCustomerProfileCommand(
        String firstName,
        String lastName,
        String email,
        String phone,
        String documentType,
        String documentNumber) {}
