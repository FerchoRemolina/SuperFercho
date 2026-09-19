package com.superfercho.assistant.application.port.out;

import java.time.Instant;

public interface ClockPort {

    Instant currentTime();
}
