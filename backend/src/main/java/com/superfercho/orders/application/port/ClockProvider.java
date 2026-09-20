package com.superfercho.orders.application.port;

import java.time.Instant;

public interface ClockProvider {

    Instant currentTime();
}
