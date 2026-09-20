package com.superfercho.orders.infrastructure.clock;

import com.superfercho.orders.application.port.ClockProvider;
import java.time.Clock;
import java.time.Instant;

public final class SystemClockAdapter implements ClockProvider {

    private final Clock clock;

    public SystemClockAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant currentTime() {
        return clock.instant();
    }
}
