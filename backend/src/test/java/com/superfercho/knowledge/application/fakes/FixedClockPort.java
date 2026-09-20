package com.superfercho.knowledge.application.fakes;

import com.superfercho.knowledge.application.port.ClockPort;
import java.time.Instant;

public final class FixedClockPort implements ClockPort {

    private Instant now;

    public FixedClockPort(Instant now) {
        this.now = now;
    }

    public void set(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }
}
