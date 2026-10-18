package com.superfercho.storefront.application.port;

import java.time.Instant;

public interface ClockPort {

    Instant currentTime();
}
