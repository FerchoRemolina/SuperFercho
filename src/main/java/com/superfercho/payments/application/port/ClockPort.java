package com.superfercho.payments.application.port;

import java.time.Instant;

public interface ClockPort {

    Instant currentTime();
}
