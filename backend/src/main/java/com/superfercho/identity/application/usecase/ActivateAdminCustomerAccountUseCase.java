package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminCustomerAccountResult;
import com.superfercho.identity.application.dto.ActivateAdminCustomerAccountCommand;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.User;
import java.time.Clock;
import java.util.Objects;

public final class ActivateAdminCustomerAccountUseCase {

    private final CustomerRecordRepository customerRecordRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ActivateAdminCustomerAccountUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository, Clock clock) {
        this.customerRecordRepository = customerRecordRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public AdminCustomerAccountResult execute(ActivateAdminCustomerAccountCommand command) {
        AdminCustomerRecordAssembler.requireRecord(customerRecordRepository, command.customerRecordId());
        User user = userRepository
                .findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));
        if (!Objects.equals(user.customerRecordId(), command.customerRecordId())) {
            throw new UserNotFoundException(command.userId());
        }
        return AdminCustomerAccountResult.from(userRepository.save(user.activate(clock.instant())));
    }
}
