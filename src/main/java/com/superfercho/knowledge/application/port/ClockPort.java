package com.superfercho.knowledge.application.port;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
