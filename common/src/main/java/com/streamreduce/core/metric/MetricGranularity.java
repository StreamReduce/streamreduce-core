/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
