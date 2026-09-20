package com.superfercho.assistant.infrastructure.identity;

import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("assistantCurrentUserProviderAdapter")
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
