package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminAccountStatusFilter;
import com.superfercho.identity.application.dto.AdminCustomerAccountResult;
import com.superfercho.identity.application.dto.AdminCustomerRecordResult;
import com.superfercho.identity.application.exception.CustomerRecordNotFoundException;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class AdminCustomerRecordAssembler {

    private AdminCustomerRecordAssembler() {}

    static AdminCustomerRecordResult assemble(
            CustomerRecord record, UserRepository userRepository, AdminAccountStatusFilter filter) {
        List<AdminCustomerAccountResult> accounts = userRepository.findAllByCustomerRecordId(record.id()).stream()
                .sorted(Comparator.comparing(User::createdAt).reversed().thenComparing(User::id))
                .filter(filter::matches)
                .map(AdminCustomerAccountResult::from)
                .toList();
        return AdminCustomerRecordResult.from(record, accounts);
    }

    static CustomerRecord requireRecord(CustomerRecordRepository records, UUID customerRecordId) {
        return records
                .findById(customerRecordId)
                .orElseThrow(() -> new CustomerRecordNotFoundException(customerRecordId));
    }

    static List<UUID> accountIds(UserRepository userRepository, UUID customerRecordId) {
        return userRepository.findAllByCustomerRecordId(customerRecordId).stream()
                .map(User::id)
                .toList();
    }
}
