package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialOrderView;
import com.superfercho.identity.application.dto.ListAdminCustomerCommercialHistoryCommand;
import com.superfercho.identity.application.port.CustomerCommercialHistoryPort;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.UserRepository;
import java.util.List;
import java.util.UUID;

public final class ListAdminCustomerOrdersUseCase {

    private final CustomerRecordRepository customerRecordRepository;
    private final UserRepository userRepository;
    private final CustomerCommercialHistoryPort commercialHistoryPort;

    public ListAdminCustomerOrdersUseCase(
            CustomerRecordRepository customerRecordRepository,
            UserRepository userRepository,
            CustomerCommercialHistoryPort commercialHistoryPort) {
        this.customerRecordRepository = customerRecordRepository;
        this.userRepository = userRepository;
        this.commercialHistoryPort = commercialHistoryPort;
    }

    public AdminPagedResult<CustomerCommercialOrderView> execute(
            ListAdminCustomerCommercialHistoryCommand command) {
        AdminCustomerRecordAssembler.requireRecord(customerRecordRepository, command.customerRecordId());
        List<UUID> customerIds =
                AdminCustomerRecordAssembler.accountIds(userRepository, command.customerRecordId());
        return commercialHistoryPort.findOrdersByCustomerIds(customerIds, command.page(), command.size());
    }
}
