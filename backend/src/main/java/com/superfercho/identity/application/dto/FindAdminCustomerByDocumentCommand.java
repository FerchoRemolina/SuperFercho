package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.validation.CustomerRegistrationRules;

public record FindAdminCustomerByDocumentCommand(
        String documentType, String documentNumber, AdminAccountStatusFilter accountStatus) {

    public static FindAdminCustomerByDocumentCommand of(
            String documentType, String documentNumber, String accountStatus) {
        CustomerRegistrationRules.requireDocument(documentType, documentNumber);
        return new FindAdminCustomerByDocumentCommand(
                documentType.trim(), documentNumber.trim(), AdminAccountStatusFilter.parse(accountStatus));
    }
}
