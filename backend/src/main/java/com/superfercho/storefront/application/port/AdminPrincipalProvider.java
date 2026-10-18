package com.superfercho.storefront.application.port;

import java.util.UUID;

/** Usuario autenticado que ejecuta una acción administrativa. */
public interface AdminPrincipalProvider {

    UUID currentAdminUserId();
}
