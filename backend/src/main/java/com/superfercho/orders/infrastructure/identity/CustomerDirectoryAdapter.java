package com.superfercho.orders.infrastructure.identity;

import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.User;
import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import com.superfercho.orders.application.port.CustomerDirectoryPort;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Bridges the admin orders list to Identity commercial data using exactly two
 * batch queries (users by ids, customer records by ids). No cross-schema SQL.
 */
@Component
@Profile("!test")
public class CustomerDirectoryAdapter implements CustomerDirectoryPort {

    private final UserRepository userRepository;
    private final CustomerRecordRepository customerRecordRepository;

    public CustomerDirectoryAdapter(
            UserRepository userRepository, CustomerRecordRepository customerRecordRepository) {
        this.userRepository = userRepository;
        this.customerRecordRepository = customerRecordRepository;
    }

    @Override
    public Map<UUID, CustomerDirectoryEntry> findByUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Collection<UUID> distinctIds = userIds.stream().distinct().toList();
        Map<UUID, CustomerRecord> recordsById = findRecordsFor(distinctIds);
        return userRepository.findAllByIds(distinctIds).stream()
                .collect(Collectors.toMap(
                        User::id,
                        user -> toEntry(user, recordsById.get(user.customerRecordId())),
                        (first, ignored) -> first));
    }

    private Map<UUID, CustomerRecord> findRecordsFor(Collection<UUID> userIds) {
        Set<UUID> recordIds = userRepository.findAllByIds(userIds).stream()
                .map(User::customerRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        return customerRecordRepository.findAllByIds(recordIds).stream()
                .collect(Collectors.toMap(CustomerRecord::id, Function.identity(), (first, ignored) -> first));
    }

    private static CustomerDirectoryEntry toEntry(User user, CustomerRecord record) {
        String fullName = record != null
                ? (record.billingFirstName() + " " + record.billingLastName()).trim()
                : (user.firstName() + " " + user.lastName()).trim();
        return new CustomerDirectoryEntry(
                user.id(),
                fullName.isBlank() ? null : fullName,
                record != null ? record.documentType() : user.documentType(),
                record != null ? record.documentNumber() : user.documentNumber(),
                user.email(),
                user.phone());
    }
}
