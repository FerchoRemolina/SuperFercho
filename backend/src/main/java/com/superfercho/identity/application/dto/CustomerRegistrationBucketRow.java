package com.superfercho.identity.application.dto;

import java.time.Instant;

/** New-customer count for one [from, to) bucket, based on CustomerRecord creation. */
public record CustomerRegistrationBucketRow(Instant bucketStart, long count) {}
