package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);
}
