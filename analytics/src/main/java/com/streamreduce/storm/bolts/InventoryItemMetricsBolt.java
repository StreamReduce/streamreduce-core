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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.streamreduce.ConnectionTypeConstants;
import com.streamreduce.ProviderIdConstants;
import com.streamreduce.analytics.MetricCriteria;
import com.streamreduce.analytics.MetricName;
import com.streamreduce.core.event.EventId;
import com.streamreduce.core.metric.MetricModeType;
import com.streamreduce.util.JSONUtils;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

/**
 * Extension of {@link AbstractMetricsBolt} that handles specialized events for the InventoryItem stream.
 */
public class InventoryItemMetricsBolt extends AbstractMetricsBolt {

    private static final Logger LOGGER = Logger.getLogger(InventoryItemMetricsBolt.class);
    private static final long serialVersionUID = 6272746264865607714L;

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(String id, Long timestamp, EventId eventId, String accountId, String userId,
                            String targetId, Map<String, Object> metadata) {
        switch (eventId) {
            case CREATE:
            case DELETE:
                String connectionId = metadata.get("targetConnectionId").toString();
                String providerType = metadata.get("targetProviderType").toString();
                String targetType = metadata.get("targetType").toString();
                String objectType = getExternalObjectType(metadata);
                Float eventValue = getEventValue(eventId);
                List<Values> metrics = new ArrayList<Values>();

                if (providerType.equals(ConnectionTypeConstants.CLOUD_TYPE)) {
                    String iso3166Code = (String) metadata.get("targetISO3166Code");
                    String region = (String) metadata.get("targetRegion");
                    String zone = (String) metadata.get("targetZone");
                    String osName = (String) metadata.get("targetOS");

                    for (String criteriaName : ImmutableSet.of("iso3166Code", "region", "zone", "osName")) {
                        MetricCriteria metricCriteria;
                        String criteriaValue;

                        if (criteriaName.equals("iso3166Code")) {
                            criteriaValue = iso3166Code;
                            metricCriteria = MetricCriteria.ISO_3166_CODE;
                        } else if (criteriaName.equals("region")) {
                            criteriaValue = region;
                            metricCriteria = MetricCriteria.REGION;
                        } else if (criteriaName.equals("zone")) {
                            criteriaValue = zone;
                            metricCriteria = MetricCriteria.AVAILABILITY_ZONE;
                        } else if (criteriaName.equals("osName")) {
                            criteriaValue = osName;
                            metricCriteria = MetricCriteria.OS_NAME;
                        } else {
                            LOGGER.error(criteriaName + " is an unsupported CloudInventoryItem metric.");
                            return;
                        }

                        // If we have an empty value, no need in processing it
                        if (!StringUtils.hasText(criteriaValue)) {
                            return;
                        }

                        // Global (metric name specific)
                        metrics.add(createGlobalMetric(MetricName.INVENTORY_ITEM_COUNT, ImmutableMap.of(
                                metricCriteria, criteriaValue
                        ), MetricModeType.DELTA, timestamp, eventValue));

                        // Account specific (metric name specific)
                        metrics.add(createAccountMetric(accountId, MetricName.INVENTORY_ITEM_COUNT, ImmutableMap.of(
                                metricCriteria, criteriaValue
                        ), MetricModeType.DELTA, timestamp, eventValue));

                        // Connection specific
                        metrics.add(createAccountMetric(accountId, MetricName.INVENTORY_ITEM_COUNT, ImmutableMap.of(
                                metricCriteria, criteriaValue,
                                MetricCriteria.CONNECTION_ID, connectionId
                        ), MetricModeType.DELTA, timestamp, eventValue));

                        if (objectType != null) {
                            // Global (metric name specific)
                            metrics.add(createGlobalMetric(MetricName.INVENTORY_ITEM_COUNT, ImmutableMap.of(
                                    metricCriteria, criteriaValue,
                                    MetricCriteria.OBJECT_TYPE, objectType
                            ), MetricModeType.DELTA, timestamp, eventValue));

                            // Account specific (metric name specific)
                            metrics.add(createAccountMetric(accountId, MetricName.INVENTORY_ITEM_COUNT, ImmutableMap.of(
                                    metricCriteria, criteriaValue,
                                    MetricCriteria.OBJECT_TYPE, objectType
                            ), MetricModeType.DELTA, timestamp, eventValue));

                            // Connection specific
                            metrics.add(createAccountMetric(accountId, MetricName.INVENTORY_ITEM_COUNT, ImmutableMap.of(
                                    metricCriteria, criteriaValue,
                                    MetricCriteria.CONNECTION_ID, connectionId,
                                    MetricCriteria.OBJECT_TYPE, objectType
                            ), MetricModeType.DELTA, timestamp, eventValue));
                        }
                    }
                }

                emitMetricsAndHashagMetrics(metrics, eventId, targetId, targetType, metadata);
                break;
            case ACTIVITY:
                String providerId = metadata.get("targetProviderId").toString();

                if (providerId.equals(ProviderIdConstants.AWS_PROVIDER_ID)) {
                    Map<String, Object> metricsPayload = (Map<String, Object>) metadata.get("payload");

                    if (metadata.containsKey("isAgentActivity") &&
                            Boolean.valueOf(metadata.get("isAgentActivity").toString())) {
                        JSONObject json = JSONUtils
                                .flattenJSON(JSONObject.fromObject(metadata.get("payload")), null);

                        for (Object rawKey : json.keySet()) {
                            String key = rawKey.toString();
                            Object value = json.get(key);

                            // elapsed_time and generated are not metrics
                            if (key.equals("elapsed_time") || key.equals("generated")) {
                                continue;
                            }

                            if (value != null && !(value instanceof JSONNull)) {
                                emitAccountMetric(accountId, MetricName.INVENTORY_ITEM_RESOURCE_USAGE, ImmutableMap.of(
                                        MetricCriteria.OBJECT_ID, targetId,
                                        MetricCriteria.RESOURCE_ID, key,
                                        MetricCriteria.METRIC_ID, key
                                ), MetricModeType.ABSOLUTE, timestamp,
                                                  Float.valueOf(json.get(key).toString()));
                            }
                        }
                    } else {
                        Set<String> knownMetricKeys = ImmutableSet.of("maximum", "minimum", "average");

                        for (Map.Entry<String, Object> metric : metricsPayload.entrySet()) {
                            String key = metric.getKey();
                            Map<String, Object> metricJson = (Map<String, Object>) metric.getValue();

                            for (String metricKey : knownMetricKeys) {
                                emitAccountMetric(accountId, MetricName.INVENTORY_ITEM_RESOURCE_USAGE, ImmutableMap.of(
                                        MetricCriteria.OBJECT_ID, targetId,
                                        MetricCriteria.RESOURCE_ID, key,
                                        MetricCriteria.METRIC_ID, metricKey
                                ), MetricModeType.ABSOLUTE, timestamp,
                                                  Float.valueOf(metricJson.get(metricKey).toString()));
                            }
                        }
                    }
                } else if (providerId.equals(ProviderIdConstants.PINGDOM_PROVIDER_ID)) {
                    Map<String, Object> payload = metadata.containsKey("payload") ?
                            (Map<String, Object>) metadata.get("payload") :
                            null;

                    if (payload != null) {
                        int lastresponsetime = payload.containsKey("lastresponsetime") ?
                                (Integer) payload.get("lastresponsetime") :
                                0;
                        emitAccountMetric(accountId, MetricName.INVENTORY_ITEM_RESOURCE_USAGE, ImmutableMap.of(
                                MetricCriteria.OBJECT_ID, targetId,
                                MetricCriteria.RESOURCE_ID, "ServerResponse",
                                MetricCriteria.METRIC_ID, "time"
                        ), MetricModeType.ABSOLUTE, timestamp, (float) lastresponsetime);
                    }
                } else if (providerId.equals(ProviderIdConstants.CUSTOM_PROVIDER_ID)) {
                    Map<String, Object> payload = metadata.containsKey("payload") ?
                            (Map<String, Object>) metadata.get("payload") :
                            null;

                    if (payload != null && payload.containsKey("metrics")) {
                        Set<Object> imgMetrics = (Set<Object>)payload.get("metrics");

                        for (Object rawMetric : imgMetrics) {
                            Map<String, Object> metric = (Map<String, Object>)rawMetric;

                            emitAccountMetric(accountId, MetricName.INVENTORY_ITEM_RESOURCE_USAGE, ImmutableMap.of(
                                    MetricCriteria.OBJECT_ID, targetId,
                                    MetricCriteria.RESOURCE_ID, (String) metric.get("name")
                            ), MetricModeType.valueOf((String) metric.get("type")), timestamp,
                                              Float.valueOf((metric.get("value")).toString()));
                        }
                    }
                } else if (providerId.equals(ProviderIdConstants.NAGIOS_PROVIDER_ID)) {
                    Map<String, Object> payload = metadata.containsKey("payload") ?
                            (Map<String, Object>) metadata.get("payload") :
                            null;

                    if (payload != null && payload.containsKey("metrics")) {
                        Set<Object> imgMetrics = (Set<Object>) payload.get("metrics");

                        for (Object rawMetric : imgMetrics) {
                            Map<String, Object> metric = (Map<String, Object>) rawMetric;
                            String[] nameTokens = ((String) metric.get("name")).split("_");

                            emitAccountMetric(accountId, MetricName.INVENTORY_ITEM_RESOURCE_USAGE, ImmutableMap.of(
                                    MetricCriteria.OBJECT_ID, targetId.toString(),
                                    MetricCriteria.RESOURCE_ID, nameTokens[0],
                                    MetricCriteria.METRIC_ID, nameTokens[1]
                            ), MetricModeType.valueOf((String) metric.get("type")), timestamp,
                                              Float.valueOf((metric.get("value")).toString()));
                        }
                    }
                }
                break;
            default:
                // Due to the nature of how built-in metrics work and such, you can end up with events
                // being passed through that are expected but unprocessed.  No need to log.
        }
    }

}
