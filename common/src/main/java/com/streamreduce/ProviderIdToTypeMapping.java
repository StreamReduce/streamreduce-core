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

import static com.streamreduce.ProviderIdConstants.*;
import static com.streamreduce.ConnectionTypeConstants.*;

public class ProviderIdToTypeMapping {
    public static final ImmutableMap<String,String> PROVIDER_IDS_TO_TYPES_MAP =
            new ImmutableMap.Builder<String,String>()
                    .put(AWS_PROVIDER_ID, CLOUD_TYPE)
                    .put(CUSTOM_PROVIDER_ID,GATEWAY_TYPE)
                    .put(NAGIOS_PROVIDER_ID,GATEWAY_TYPE)
                    .put(FEED_PROVIDER_ID,FEED_TYPE)
                    .put(GITHUB_PROVIDER_ID,PROJECT_HOSTING_TYPE)
                    .put(JIRA_PROVIDER_ID,PROJECT_HOSTING_TYPE)
                    .put(PINGDOM_PROVIDER_ID,MONITORING_TYPE)
                    .put(TWITTER_PROVIDER_ID,SOCIAL_TYPE)
                    .put(WEBHDFS_PROVIDER_ID,GATEWAY_TYPE)
                    .build();
}
