package com.streamreduce.core.metric;

import java.util.concurrent.TimeUnit;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/27/12 14:16</p>
 */
public enum MetricGranularity {

    MINUTE(TimeUnit.MINUTES.toMillis(1)),
    QUARTER_HOUR(TimeUnit.MINUTES.toMillis(15)),
    HALF_HOUR(TimeUnit.MINUTES.toMillis(30)),
    HOUR(TimeUnit.HOURS.toMillis(1)),
    SHIFT(TimeUnit.HOURS.toMillis(8)),
    DAY(TimeUnit.DAYS.toMillis(1)),
    WEEK(TimeUnit.DAYS.toMillis(7)),
    MONTH(TimeUnit.DAYS.toMillis(30));

    private long millis;

    MetricGranularity(long millis) {
        this.millis = millis;
    }

    public long getMillis() {
        return millis;
    }

    public static MetricGranularity fromValue(long millis) {
        for (MetricGranularity granularity : values()) {
            if (granularity.millis == millis) {
                return granularity;
            }
        }
        throw new IllegalArgumentException("Millis value "+millis+" does not match any MetricGranularity");
    }

}
