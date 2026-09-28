package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.CustomerAccountAccessPort;
import com.superfercho.identity.application.port.UserRepository;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CustomerAccountAccessAdapter implements CustomerAccountAccessPort {

    private final UserRepository userRepository;

    public CustomerAccountAccessAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean allowsCustomerAccess(UUID userId) {
        return userRepository
                .findById(userId)
                .map(user -> user.deletedAt() == null)
                .orElse(false);
    }
}
