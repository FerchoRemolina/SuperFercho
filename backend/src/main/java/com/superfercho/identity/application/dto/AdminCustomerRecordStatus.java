package com.superfercho.identity.application.dto;

/**
 * Estado administrativo derivado del conjunto de cuentas de un CustomerRecord
 * (no se persiste). No confundir con {@link AdminAccountStatusFilter}, que filtra
 * cuentas individuales en el detalle.
 *
 * <ul>
 *   <li>ACTIVE: ≥1 User vivo ACTIVE
 *   <li>INACTIVE: hay Users vivos, ninguno ACTIVE (todos INACTIVE)
 *   <li>CLOSED: no hay User vivo y existe ≥1 User con deletedAt
 *   <li>NO_ACCOUNT: no existe ningún User vinculado
 * </ul>
 */
public enum AdminCustomerRecordStatus {
    ACTIVE,
    INACTIVE,
    CLOSED,
    NO_ACCOUNT;

    public static AdminCustomerRecordStatus derive(
            long activeAccounts, long inactiveAccounts, long deletedAccounts) {
        if (activeAccounts > 0) {
            return ACTIVE;
        }
        if (inactiveAccounts > 0) {
            return INACTIVE;
        }
        if (deletedAccounts > 0) {
            return CLOSED;
        }
        return NO_ACCOUNT;
    }
}
