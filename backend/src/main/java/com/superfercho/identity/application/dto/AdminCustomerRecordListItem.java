package com.superfercho.identity.application.dto;

import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

/** Fila del listado administrativo de clientes: identidad comercial +
 *  resumen de cuentas + métricas comerciales derivadas de pedidos. */
public record AdminCustomerRecordListItem(
        UUID id,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        AdminCustomerRecordStatus status,
        long accountCount,
        long activeAccounts,
        long inactiveAccounts,
        long deletedAccounts,
        long orderCount,
        Money totalSpent,
        Instant lastOrderAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminCustomerRecordStatus deriveStatus(
            long activeAccounts, long inactiveAccounts, long deletedAccounts) {
        return AdminCustomerRecordStatus.derive(activeAccounts, inactiveAccounts, deletedAccounts);
    }
}
