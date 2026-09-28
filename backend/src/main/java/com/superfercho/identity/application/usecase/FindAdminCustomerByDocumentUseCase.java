package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminCustomerRecordResult;
import com.superfercho.identity.application.dto.FindAdminCustomerByDocumentCommand;
import com.superfercho.identity.application.exception.CustomerRecordNotFoundException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerRecord;

public final class FindAdminCustomerByDocumentUseCase {

    private final CustomerRecordRepository customerRecordRepository;
    private final UserRepository userRepository;

    public FindAdminCustomerByDocumentUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository) {
        this.customerRecordRepository = customerRecordRepository;
        this.userRepository = userRepository;
    }

    public AdminCustomerRecordResult execute(FindAdminCustomerByDocumentCommand command) {
        CustomerRecord record = customerRecordRepository
                .findByDocument(command.documentType(), command.documentNumber())
                .orElseThrow(() -> new CustomerRecordNotFoundException(
                        command.documentType(), command.documentNumber()));
        return AdminCustomerRecordAssembler.assemble(record, userRepository, command.accountStatus());
    }
}
