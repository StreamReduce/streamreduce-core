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

package com.streamreduce.analytics;

/**
 * Enumeration to describe the possible metrics names to send to the Juggaloader.
 */
public enum MetricName {

    ACCOUNT_COUNT,
    CONNECTION_ACTIVITY_COUNT,
    CONNECTION_COUNT,
    CONNECTION_RESOURCE_USAGE,
    INVENTORY_ITEM_ACTIVITY_COUNT,
    INVENTORY_ITEM_COUNT,
    INVENTORY_ITEM_RESOURCE_USAGE,
    MESSAGE_COUNT,
    PENDING_USER_COUNT,
    TEST_STREAM,
    USER_COUNT;

    public static MetricName fromValue(String metricName) {
        for (MetricName name : values()) {
            if (name.name().equals(metricName)) {
                return name;
            }
        }
        throw new IllegalArgumentException("Passed metric name value "+metricName+" does not match any existing metric names");
    }
}
