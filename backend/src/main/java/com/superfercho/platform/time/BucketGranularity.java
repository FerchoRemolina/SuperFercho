package com.superfercho.platform.time;

/**
 * Fixed bucket sizes for dashboard time series. Bucket boundaries are computed in
 * {@link BusinessZone#BOGOTA}: HOUR truncates to the clock hour, DAY to the Bogota
 * calendar day and MONTH to the first day of the Bogota calendar month.
 */
public enum BucketGranularity {
    HOUR,
    DAY,
    MONTH
}
