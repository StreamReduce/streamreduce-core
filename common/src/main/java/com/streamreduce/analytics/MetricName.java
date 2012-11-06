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
