package com.superfercho.orders.infrastructure.identity;

import com.superfercho.orders.application.port.CurrentUserProvider;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CurrentUserProviderAdapter implements CurrentUserProvider {

    private final com.superfercho.identity.application.port.CurrentUserProvider identityCurrentUserProvider;

    public CurrentUserProviderAdapter(
            com.superfercho.identity.application.port.CurrentUserProvider identityCurrentUserProvider) {
        this.identityCurrentUserProvider = identityCurrentUserProvider;
    }

    @Override
    public UUID getCurrentUserId() {
        return identityCurrentUserProvider.getCurrentUserId();
    }
}
