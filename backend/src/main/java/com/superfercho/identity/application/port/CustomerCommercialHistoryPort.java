package com.superfercho.identity.application.port;

import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialOrderView;
import com.superfercho.identity.application.dto.CustomerCommercialPaymentView;
import java.util.Collection;
import java.util.UUID;

/**
 * Cross-module commercial history for admin CustomerRecord views. Implemented by Orders (orders +
 * payment lookups via Orders' PaymentPort).
 */
public interface CustomerCommercialHistoryPort {

    AdminPagedResult<CustomerCommercialOrderView> findOrdersByCustomerIds(
            Collection<UUID> customerIds, Integer page, Integer size);

    AdminPagedResult<CustomerCommercialPaymentView> findPaymentsByCustomerIds(
            Collection<UUID> customerIds, Integer page, Integer size);
}
