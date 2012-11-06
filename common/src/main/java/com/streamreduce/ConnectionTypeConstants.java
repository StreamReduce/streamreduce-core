package com.streamreduce;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public final class ConnectionTypeConstants {

    public static final String ANALYTICS_TYPE = "analytics";
    public static final String CLOUD_TYPE = "cloud";
    public static final String PROJECT_HOSTING_TYPE = "projecthosting";
    public static final String FEED_TYPE = "feed";
    public static final String GATEWAY_TYPE = "gateway";
    public static final String MONITORING_TYPE = "monitoring";
    public static final String SOCIAL_TYPE = "social";
    public static final Set<String> ALL_CONNECTION_TYPES  = ImmutableSet.of(
            CLOUD_TYPE,
            PROJECT_HOSTING_TYPE,
            FEED_TYPE,
            GATEWAY_TYPE,
            MONITORING_TYPE,
            SOCIAL_TYPE
    );

    private ConnectionTypeConstants() {
    }
}
