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
