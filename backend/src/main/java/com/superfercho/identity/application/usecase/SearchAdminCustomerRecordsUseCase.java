package com.superfercho.identity.application.usecase;

import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import java.util.Objects;

/** Búsqueda paginada de CustomerRecords para el listado administrativo. */
public final class SearchAdminCustomerRecordsUseCase {

    private final CustomerRecordRepository customerRecordRepository;

    public SearchAdminCustomerRecordsUseCase(CustomerRecordRepository customerRecordRepository) {
        this.customerRecordRepository = customerRecordRepository;
    }

    public AdminCustomerRecordsPage execute(AdminCustomerRecordSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        return customerRecordRepository.searchRecords(criteria);
    }
}
