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

import static com.streamreduce.feed.types.FeedType.RSS;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class ProviderIdConstants {

    private ProviderIdConstants() {}

    public static final String AWS_PROVIDER_ID = "aws";
    public static final String CUSTOM_PROVIDER_ID = "custom";
    public static final String FEED_PROVIDER_ID = RSS.toString();
    public static final String GITHUB_PROVIDER_ID = "github";
    public static final String GOOGLE_ANALYTICS_PROVIDER_ID = "googleanalytics";
    public static final String JIRA_PROVIDER_ID = "jira";
    public static final String NAGIOS_PROVIDER_ID = "nagios";
    public static final String PINGDOM_PROVIDER_ID = "pingdom";
    public static final String TWITTER_PROVIDER_ID = "twitter";
    public static final String WEBHDFS_PROVIDER_ID = "webhdfs";
    public static final Set<String> ALL_PROVIDER_IDS = ImmutableSet.of(
            AWS_PROVIDER_ID,
            CUSTOM_PROVIDER_ID,
            FEED_PROVIDER_ID,
            GITHUB_PROVIDER_ID,
            JIRA_PROVIDER_ID,
            NAGIOS_PROVIDER_ID,
            PINGDOM_PROVIDER_ID,
            TWITTER_PROVIDER_ID,
            WEBHDFS_PROVIDER_ID
    );

}
