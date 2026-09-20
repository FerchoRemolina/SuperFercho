package com.superfercho.shopping.infrastructure.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemClockAdapterTest {

    @Test
    void shouldReturnInstantFromClock() {
        Instant fixed = Instant.parse("2026-04-01T10:00:00Z");
        SystemClockAdapter adapter = new SystemClockAdapter(Clock.fixed(fixed, ZoneOffset.UTC));

        assertEquals(fixed, adapter.currentTime());
    }
}
