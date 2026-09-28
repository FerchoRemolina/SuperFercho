package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminCustomerRecordResult;
import com.superfercho.identity.application.dto.GetAdminCustomerRecordCommand;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerRecord;

public final class GetAdminCustomerRecordUseCase {

    private final CustomerRecordRepository customerRecordRepository;
    private final UserRepository userRepository;

    public GetAdminCustomerRecordUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository) {
        this.customerRecordRepository = customerRecordRepository;
        this.userRepository = userRepository;
    }

    public AdminCustomerRecordResult execute(GetAdminCustomerRecordCommand command) {
        CustomerRecord record =
                AdminCustomerRecordAssembler.requireRecord(customerRecordRepository, command.customerRecordId());
        return AdminCustomerRecordAssembler.assemble(record, userRepository, command.accountStatus());
    }
}
