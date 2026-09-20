package com.superfercho.shopping.infrastructure.clock;

import com.superfercho.shopping.application.port.out.ClockPort;
import java.time.Clock;
import java.time.Instant;

public final class SystemClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemClockAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant currentTime() {
        return clock.instant();
    }
}
