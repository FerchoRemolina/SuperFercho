package com.superfercho.assistant.application.packagesel;

import java.time.Duration;

public final class PackageSelectionDurations {

    public static final Duration PROPOSAL_TTL = Duration.ofMinutes(15);

    public static final Duration CONSUMING_LEASE = Duration.ofSeconds(15);

    private PackageSelectionDurations() {}
}
