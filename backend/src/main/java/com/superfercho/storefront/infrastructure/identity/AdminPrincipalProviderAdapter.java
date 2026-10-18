package com.superfercho.storefront.infrastructure.identity;

import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.storefront.application.port.AdminPrincipalProvider;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AdminPrincipalProviderAdapter implements AdminPrincipalProvider {

    private final CurrentUserProvider currentUserProvider;

    public AdminPrincipalProviderAdapter(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public UUID currentAdminUserId() {
        return currentUserProvider.getCurrentUserId();
    }
}
