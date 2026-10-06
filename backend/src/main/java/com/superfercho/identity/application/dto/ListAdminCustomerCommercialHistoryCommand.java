package com.superfercho.identity.application.dto;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import java.util.UUID;

/** Historial comercial de un CustomerRecord. page/size con defaults y tope
 *  para evitar NPE en PageRequest y descargas sin límite. */
public record ListAdminCustomerCommercialHistoryCommand(
        UUID customerRecordId, Integer page, Integer size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public ListAdminCustomerCommercialHistoryCommand {
        if (customerRecordId == null) {
            throw new InvalidAdminCustomerQueryException("customerRecordId is required");
        }
        page = page == null || page < 0 ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));
    }
}
