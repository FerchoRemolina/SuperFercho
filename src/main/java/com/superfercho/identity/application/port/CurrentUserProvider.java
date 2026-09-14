package com.superfercho.identity.application.port;

import java.util.UUID;

public interface CurrentUserProvider {

    UUID getCurrentUserId();
}
