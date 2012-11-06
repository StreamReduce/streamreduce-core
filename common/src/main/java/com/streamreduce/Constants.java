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

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public final class Constants {

    private Constants() {
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("application");

    /* If IIIIIIiiiiiiiii could put time in a bottle... */
    public static final long PERIOD_MINUTE = TimeUnit.MINUTES.toMillis(1);
    public static final long PERIOD_HOUR = TimeUnit.HOURS.toMillis(1);
    public static final long PERIOD_DAY = TimeUnit.DAYS.toMillis(1);
    public static final long PERIOD_WEEK = TimeUnit.DAYS.toMillis(7);
    public static final long PERIOD_MONTH = TimeUnit.DAYS.toMillis(30);
    public static final Map<String, Long> KNOWN_GRANULARITY_NAMES = ImmutableMap.of(
            "MINUTES", PERIOD_MINUTE,
            "HOURS", PERIOD_HOUR,
            "DAYS", PERIOD_DAY,
            "WEEKS", PERIOD_WEEK,
            "MONTHS", PERIOD_MONTH
    );

    /* Custom header key that contains the authentication token */
    public static final String NODEABLE_AUTH_TOKEN = "X-Auth-Token";

    public static final String NODEABLE_API_KEY = "X-Auth-API-Key";

    public static final String NODEABLE_HTTP_USER_AGENT = "Nodeable HTTP Client";

    /* default max number of messages to retrieve when no "limit" param is sent */
    public static final int DEFAULT_MAX_MESSAGES = 100;

    /* prefix for the account message inbox mongo collection */
    public static final String INBOX_COLLECTION_PREFIX = "Inbox_";

    /* prefix for the account metric inbox mongo collection */
    public static final String METRIC_COLLECTION_PREFIX = "Metric_";

    /* 2 weeks */
    public static final long BOOTSTRAP_MESSAGE_ARCHIVE_DURATION = (PERIOD_WEEK);

    /* internal use only "super user account" */
    public static final String NODEABLE_SUPER_ACCOUNT_NAME = resourceBundle.getString("nodeable.superuser.account");
    public static final String NODEABLE_SUPER_USERNAME = resourceBundle.getString("nodeable.superuser.username");
    public static final String NODEABLE_SYSTEM_USERNAME = resourceBundle.getString("nodeable.system.username");

    public static final int MAX_MESSAGE_LENGTH = 3000;

    /* MongoDB Collection Names */
    public static final String INVENTORY_ITEM_METADATA_COLLECTION_NAME = "inventoryItemMetadata";

    /* InventoryItem types */
    public static final String BUCKET_TYPE = "bucket";
    public static final String COMPUTE_INSTANCE_TYPE = "compute";
    public static final String PROJECT_TYPE = "project";
    public static final String CUSTOM_TYPE = "custom";
    public static final String MONITOR_TYPE = "monitor";
    public static final String ANALYTICS_TYPE = "analytics";

}
