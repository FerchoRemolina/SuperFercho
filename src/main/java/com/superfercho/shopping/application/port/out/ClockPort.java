package com.superfercho.shopping.application.port.out;

import java.time.Instant;

public interface ClockPort {

    Instant currentTime();
}
