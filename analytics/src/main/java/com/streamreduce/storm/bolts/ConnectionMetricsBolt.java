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

package com.streamreduce.storm.bolts;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.analytics.MetricCriteria;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.metric.MetricModeType;


/**
 * Extension of {@link AbstractMetricsBolt} that handles specialized events for the Connection stream.
 */
public class ConnectionMetricsBolt extends AbstractMetricsBolt {

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(String id, Long timestamp, EventId eventId, String accountId, String userId,
                            String targetId, Map<String, Object> metadata) {

        switch (eventId) {
            case ACTIVITY:
                String providerId = metadata.get("targetProviderId").toString();

                if (providerId.equals(ProviderIdConstants.CUSTOM_PROVIDER_ID)) {
                    Map<String, Object> payload = metadata.containsKey("payload") ?
                            (Map<String, Object>) metadata.get("payload") :
                            null;

                    if (payload != null && payload.containsKey("metrics")) {
                        Set<Object> metrics = (Set<Object>)payload.get("metrics");

                        for (Object rawMetric : metrics) {
                            Map<String, Object> metric = (Map<String, Object>)rawMetric;

                            emitAccountMetric(accountId, MetricName.CONNECTION_RESOURCE_USAGE, ImmutableMap.of(
                                    MetricCriteria.OBJECT_ID, targetId.toString(),
                                    MetricCriteria.RESOURCE_ID, (String)metric.get("name")
                            ), MetricModeType.valueOf((String) metric.get("type")), timestamp,
                                              ((Number)metric.get("value")).floatValue());
                        }
                    }
                } else if (providerId.equals(ProviderIdConstants.TWITTER_PROVIDER_ID)) {
                    Map<String, Object> payload = metadata.containsKey("payload") ?
                            (Map<String, Object>) metadata.get("payload") :
                            null;

                    if (payload != null) {
                        int favoritesCount = payload.containsKey("favourites_count") ?
                                (Integer)payload.get("favourites_count") :
                                0;
                        int followersCount = payload.containsKey("followers_count") ?
                                (Integer)payload.get("followers_count") :
                                0;
                        int friendsCount = payload.containsKey("friends_count") ?
                                (Integer)payload.get("friends_count") :
                                0;
                        int listedCount = payload.containsKey("listed_count") ?
                                (Integer)payload.get("listed_count") :
                                0;
                        int statusesCount = payload.containsKey("statuses_count") ?
                                (Integer)payload.get("statuses_count") :
                                0;

                        emitAccountMetric(accountId, MetricName.CONNECTION_RESOURCE_USAGE,
                                          ImmutableMap.of(
                                                  MetricCriteria.OBJECT_ID, targetId.toString(),
                                                  MetricCriteria.RESOURCE_ID, "Favorites Count"
                                          ), MetricModeType.ABSOLUTE, timestamp, (float)favoritesCount);

                        emitAccountMetric(accountId, MetricName.CONNECTION_RESOURCE_USAGE,
                                          ImmutableMap.of(
                                                  MetricCriteria.OBJECT_ID, targetId.toString(),
                                                  MetricCriteria.RESOURCE_ID, "Followers Count"
                                          ), MetricModeType.ABSOLUTE, timestamp, (float)followersCount);

                        emitAccountMetric(accountId, MetricName.CONNECTION_RESOURCE_USAGE,
                                          ImmutableMap.of(
                                                  MetricCriteria.OBJECT_ID, targetId.toString(),
                                                  MetricCriteria.RESOURCE_ID, "Following Count"
                                          ), MetricModeType.ABSOLUTE, timestamp, (float)friendsCount);

                        emitAccountMetric(accountId, MetricName.CONNECTION_RESOURCE_USAGE,
                                          ImmutableMap.of(
                                                  MetricCriteria.OBJECT_ID, targetId.toString(),
                                                  MetricCriteria.RESOURCE_ID, "Listed Count"
                                          ), MetricModeType.ABSOLUTE, timestamp, (float)listedCount);

                        emitAccountMetric(accountId, MetricName.CONNECTION_RESOURCE_USAGE,
                                          ImmutableMap.of(
                                                  MetricCriteria.OBJECT_ID, targetId.toString(),
                                                  MetricCriteria.RESOURCE_ID, "Statuses Count"
                                          ), MetricModeType.ABSOLUTE, timestamp, (float)statusesCount);
                    }
                }

                break;
            default:
                // Due to the nature of how built-in metrics work and such, you can end up with events
                // being passed through that are expected but unprocessed.  No need to log.
        }
    }


}
