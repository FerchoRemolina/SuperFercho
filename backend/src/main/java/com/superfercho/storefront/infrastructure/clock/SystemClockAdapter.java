package com.superfercho.storefront.infrastructure.clock;

import com.superfercho.storefront.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
public class SystemClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemClockAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant currentTime() {
        return clock.instant();
    }
}
